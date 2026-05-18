# floating-ball-server API 说明

> 更新日期：2026-05-11
> 范围：`floating-ball` 区域化模式下调用的远端 `/v1/*` 接口

## 1. 约束说明

1. 本文档只描述远端接口，不包含 `floating-ball` 本地 `/api/consultation/*`。
2. 接口实现必须同时兼容以下调用方：
   - `floating-ball/src/services/regionalClient.ts`
   - `floating-ball/src/services/llm.ts`
   - `floating-ball/src/services/templateService.ts`
   - `floating-ball/src/services/medicalData.ts`
   - `floating-ball/src/services/promptOverride.ts`
   - `floating-ball/src/services/auditUploader.ts`
3. 安全模式下，仓库内 `application.yml` 不再内置真实数据库地址、账号口令或 AES key；部署前必须注入 `FB_DB_URL`、`FB_DB_USERNAME`、`FB_DB_PASSWORD`、`FB_AES_KEY`。
4. 统一响应结构如下：

```json
{
  "code": "0",
  "message": "success",
  "data": {},
  "requestId": "uuid",
  "timestamp": 1770000000000
}
```

## 2. 认证与签名

### 2.1 客户端接口

- 路径前缀：`/v1/*`
- 认证方式：`Authorization: Bearer {deviceToken}` + ECDSA P-256 请求签名
- 例外：`POST /v1/client/register` 与 `/v1/client/releases/*` 无需设备令牌和请求签名

除上述例外外，所有 `/v1/*` HTTP 请求必须携带：

| Header | 必填 | 说明 |
| --- | --- | --- |
| Authorization | 是 | `Bearer {deviceToken}` |
| X-Timestamp | 是 | 毫秒时间戳，服务端默认允许 5 分钟时钟偏移 |
| X-Nonce | 是 | 每次请求唯一随机数，服务端会拒绝重放 |
| X-Signature | 是 | ECDSA P-256 / SHA-256 签名，base64 编码 |
| X-Body-SHA256 | 有 body 时是 | 请求体 SHA-256 hex；服务端会用实际 body 重算并比对 |

签名原文固定为：

```text
METHOD
PATH
TIMESTAMP
NONCE
BODY_SHA256
```

说明：

1. `METHOD` 使用大写 HTTP 方法，例如 `POST`。
2. `PATH` 只包含路径，不包含 query，例如 `/v1/ai/chat`。
3. `BODY_SHA256` 必须使用实际请求体字节计算；空 body 使用空字符串 SHA-256：`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`。
4. 服务端不会信任客户端声明的 `X-Body-SHA256`，会用实际收到的 body 重算并参与验签。
5. 签名失败返回 `SIG-401`；令牌缺失或无效返回 `AUTH-401`；客户端版本过低返回 `UPDATE-REQUIRED`。

### 2.2 管理端接口

- 当前采用 `Authorization: Bearer {adminToken}` 方案
- 例外：`POST /admin/api/auth/login` 无需鉴权
- `GET /admin/api/auth/me` 用于管理端恢复当前登录用户
- `PUT /admin/api/auth/password` 用于当前管理员登录后修改自己的密码
- 管理端页面默认由同一 Spring Boot 服务托管，入口为 `/admin/`
- 浏览器访问管理端时，默认采用同源调用；CORS 仅保留给本地独立调试使用

### 2.3 管理员密码维护

#### 2.3.1 PUT `/admin/api/auth/password`

用途：当前已登录管理员修改自己的密码。

请求：

```json
{
  "oldPassword": "admin123",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

约束：

- 需要携带有效 `Authorization: Bearer {adminToken}`
- `oldPassword` 必须与当前账号现有密码匹配
- `newPassword` 与 `confirmPassword` 必须一致
- 新密码长度至少 6 位，且不能与旧密码相同

响应 `data`：

```json
{
  "status": "ok"
}
```

#### 2.3.2 启动期受控密码重置

用途：在管理员忘记密码、无法登录时，通过服务启动配置重置指定账号密码。

配置项：

- `floating-ball.admin.bootstrap-reset.enabled`
- `floating-ball.admin.bootstrap-reset.username`
- `floating-ball.admin.bootstrap-reset.password`

环境变量映射：

- `FB_ADMIN_BOOTSTRAP_RESET_ENABLED`
- `FB_ADMIN_BOOTSTRAP_RESET_USERNAME`
- `FB_ADMIN_BOOTSTRAP_RESET_PASSWORD`

约束：

- 仅在服务启动阶段执行，不提供公开匿名 HTTP 接口
- 默认账号可填 `admin`，重置完成后应立即关闭该配置并重启服务

## 3. 客户端版本发布与内网更新

### 3.1 管理端上传客户端版本

`POST /admin/api/releases/upload`

用途：管理员上传 Tauri updater 可识别的客户端安装包、签名和版本元数据，用于内网环境发布桌面端更新。

鉴权：`Authorization: Bearer {adminToken}`

请求类型：`multipart/form-data`

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| channel | string | 是 | 发布通道：`production` 正式内网，`testing` 测试内网 |
| metadataFile | file | 是 | Tauri 发布产物中的 `latest.json`，服务端从中解析 `version`、`platforms.{target}.signature`、`notes`、`pub_date` |
| version | string | 否 | 客户端版本号；默认从 `metadataFile.version` 读取，手工填写时覆盖文件值 |
| target | string | 否 | Tauri updater target；默认按安装包文件名匹配 `metadataFile.platforms`，多平台无法匹配时需手工填写 |
| signature | string | 否 | 对安装包生成的 Tauri/minisign 签名内容；默认从 `metadataFile.platforms.{target}.signature` 读取 |
| notes | string | 否 | 更新说明；默认从 `metadataFile.notes` 读取，手工填写时覆盖文件值 |
| pubDate | string | 否 | 发布时间，ISO-8601；默认从 `metadataFile.pub_date` 读取，均为空时由服务端生成 |
| forceUpdate | boolean | 否 | 是否强制更新；为 `true` 时，低于本次发布版本的客户端只能访问更新检查和安装包下载 |
| file | file | 是 | 安装包或更新包文件，通常为 Tauri bundle 产物 |

说明：运维推荐只选择 `latest.json` 与对应安装包文件；`version`、`target`、`signature` 仅作为解析失败或多平台歧义时的兜底覆盖项。安装包文件名必须与 `latest.json.platforms.{target}.url` 中的文件名一致，例如签名对应 `MedHermes.app.tar.gz` 时不能上传 `MedHermes.dmg`，否则 Tauri updater 会签名校验失败。勾选强制更新前，必须确认该通道所有实际部署平台的安装包均已上传到当前 `latest.json`，否则旧客户端会被禁止使用但无法下载对应平台更新。

部署说明：生产/内网环境推荐设置 `FB_RELEASE_PUBLIC_BASE_URL=http://后端内网IP:8080`，确保管理端展示、复制的更新源以及 `latest.json` 内下载地址都不出现 `localhost`。

响应 `data`：

```json
{
  "channel": "production",
  "version": "1.2.13",
  "target": "darwin-aarch64",
  "fileName": "MedHermes_1.2.13_aarch64.dmg",
  "fileSize": 12345678,
  "downloadUrl": "http://127.0.0.1:8080/v1/client/releases/production/files/darwin-aarch64/MedHermes_1.2.13_aarch64.dmg",
  "latestJsonUrl": "http://127.0.0.1:8080/v1/client/releases/production/latest.json",
  "policyUrl": "http://127.0.0.1:8080/v1/client/releases/production/policy.json",
  "pubDate": "2026-04-24T10:00:00Z",
  "forceUpdate": true,
  "minSupportedVersion": "1.2.13"
}
```

### 3.2 管理端查询发布状态

`GET /admin/api/releases?channel=production`

用途：返回指定通道当前可见版本；`channel` 为空时返回所有通道。

### 3.3 管理端切换强制更新策略

`POST /admin/api/releases/policy`

用途：独立开启或关闭当前通道的强制更新策略，不需要重新上传安装包。

请求：

```json
{
  "channel": "production",
  "forceUpdate": true
}
```

响应 `data`：同 `GET /admin/api/releases` 的单条 `ReleaseView`。

说明：

1. 开启强制更新时，`minSupportedVersion` 固定为当前通道正在发布的 `latestVersion`。
2. 关闭强制更新时，`minSupportedVersion` 清空，旧客户端不再因版本低被业务接口拦截。
3. 切换策略会同步更新当前版本的历史快照，确保后续回滚能恢复该版本当时的策略状态。

### 3.4 管理端查询历史版本

`GET /admin/api/releases/history?channel=production`

用途：返回指定通道的历史发布快照；`channel` 为空时返回所有通道。历史快照由服务端在上传与回滚时自动维护，不依赖数据库表。

响应 `data`：

```json
[
  {
    "channel": "production",
    "version": "1.2.13",
    "active": false,
    "forceUpdate": false,
    "minSupportedVersion": null,
    "targets": ["darwin-aarch64", "windows-x86_64"],
    "fileNames": ["MedHermes_1.2.13_aarch64.app.tar.gz", "MedHermes_1.2.13_x64-setup.nsis.zip"],
    "notes": "修复内网升级流程",
    "pubDate": "2026-04-24T10:00:00Z",
    "updatedAt": 1777250000000
  }
]
```

### 3.5 管理端回滚到历史版本

`POST /admin/api/releases/rollback`

用途：把指定通道的当前发布回滚到历史快照，同时恢复该版本当时的强制更新策略。回滚只切换 `latest.json` / `policy.json` 指针，不重新上传安装包。

请求：

```json
{
  "channel": "production",
  "version": "1.2.13"
}
```

响应 `data`：同 `GET /admin/api/releases` 的单条 `ReleaseView`。

说明：

1. 回滚前服务端会校验历史快照中每个平台的安装包文件仍存在。
2. 如果当前发布目录中没有历史快照，服务端会把当前 `latest.json` 作为兼容历史记录展示；但只有已写入快照的版本才能执行回滚。
3. 从一个版本切换到另一个版本时，服务端会先保存当前版本快照，确保刚发错的新版本也能被再次回滚回来。

### 3.6 客户端检查更新策略

`GET /v1/client/releases/{channel}/policy.json`

用途：公开返回指定通道的客户端更新策略。该接口不需要设备令牌，强制更新状态下仍允许旧客户端访问。

示例：

```json
{
  "channel": "production",
  "latestVersion": "1.2.13",
  "forceUpdate": true,
  "minSupportedVersion": "1.2.13",
  "latestJsonUrl": "http://127.0.0.1:8080/v1/client/releases/production/latest.json",
  "notes": "修复内网升级流程",
  "pubDate": "2026-04-24T10:00:00Z",
  "updatedAt": 1777250000000
}
```

说明：

1. `forceUpdate=false` 时，客户端可提示可选更新，但服务端不因版本较低拦截业务接口。
2. `forceUpdate=true` 时，低于 `minSupportedVersion` 的客户端只能访问 `/v1/client/releases/**`。
3. 桌面端每个 `/v1/*` 业务请求应携带 `X-Client-Version` 与 `X-Update-Channel`；服务端在缺失请求头时回退设备表 `client_version` 与 `production` 通道策略。

强制更新拦截响应：

```http
HTTP/1.1 426 Upgrade Required
Content-Type: application/json
```

```json
{
  "code": "UPDATE-REQUIRED",
  "message": "当前客户端版本过低，请升级到 1.2.13 或更高版本后继续使用",
  "data": null,
  "requestId": "..."
}
```

### 3.7 客户端检查更新元数据

`GET /v1/client/releases/{channel}/latest.json`

用途：公开返回 Tauri updater 兼容的 `latest.json`。该接口不需要设备令牌，避免 updater 无法附带自定义鉴权头。

当指定通道尚未通过管理端上传任何可用版本时，接口返回 `204 No Content`，表示当前没有可用更新，不记录业务异常。

示例：

```json
{
  "version": "1.2.13",
  "notes": "修复内网升级流程",
  "pub_date": "2026-04-24T10:00:00Z",
  "platforms": {
    "darwin-aarch64": {
      "signature": "...",
      "url": "http://127.0.0.1:8080/v1/client/releases/production/files/darwin-aarch64/MedHermes_1.2.13_aarch64.dmg"
    }
  }
}
```

### 3.8 客户端下载安装包

`GET /v1/client/releases/{channel}/files/{target}/{fileName}`

用途：公开下载指定通道和平台的安装包文件，供 Tauri updater 下载。

## 4. 客户端接口

### 4.1 POST `/v1/client/register`

用途：客户端首次启动时注册设备。

请求：

```json
{
  "cdDevice": "9C:4E:36:AA:BB:CC",
  "naDevice": "FloatingBall-win32",
  "cdOrg": "ORG001",
  "clientVersion": "0.1.0",
  "updateChannel": "production",
  "osInfo": "Windows 10",
  "publicKey": "base64-spki-public-key"
}
```

说明：

1. 当前桌面端优先使用设备 MAC 地址作为 `cdDevice`；仅在当前环境无法读取 MAC 时才回退到本地兜底编码。
2. `publicKey` 为 ECDSA P-256 公钥，SPKI DER 后 base64 编码。
3. 已存在且已绑定公钥的设备不允许匿名重新注册接管；若本地 token/key 丢失，客户端应生成新的兜底 `cdDevice` 注册为新设备，避免弱运维场景下阻断医生使用。
4. 已存在但未绑定公钥的历史设备允许在注册时补录公钥，用于兼容旧数据。

响应 `data`：

```json
{
  "idDevice": "uuid",
  "deviceToken": "32-char-token",
  "heartbeatInterval": 30,
  "hasPublicKey": true
}
```

### 3.2 POST `/v1/client/heartbeat`

用途：设备心跳保活。

请求体允许为空，兼容当前客户端也允许带 `idDevice`。

响应 `data`：

```json
{
  "status": "ok",
  "serverTime": 1770000000000
}
```

### 3.3 GET `/v1/client/bootstrap`

用途：返回当前设备可见的 AI 配置、功能开关和版本号。

约束：

1. 只返回桌面端需要感知的非敏感配置
2. `apiKey`、审查模型密钥、PMPHAI `appKey/appSecret` 不得出现在响应中

响应 `data` 结构必须兼容 `regionalClient.ts`：

```json
{
  "llm": {
    "baseUrl": "https://example.com/v1",
    "model": "deepseek-chat",
    "fastModel": "deepseek-chat-lite",
    "enableThinking": true,
    "audioBaseUrl": "https://example.com/v1",
    "audioModel": "whisper-1"
  },
  "speech": {
    "provider": "openai-compatible",
    "model": "whisper-1"
  },
  "knowledgeBase": {
    "enabled": true,
    "baseUrl": "https://pmphai.example.com"
  },
  "pmphai": {
    "enabled": true
  },
  "reviewer": {
    "enabled": false,
    "model": "gpt-4o-mini",
    "checkExaminationEnabled": true
  },
  "features": {
    "regionalMode": true,
    "aiProxyEnabled": true,
    "auditEnabled": true
  },
  "templateVersion": "2026.04.20.1",
  "dataPackageVersion": "2026.04.20.1",
  "promptVersion": "2026.04.20.1"
}
```

语音配置字段说明：

- `llm.fastModel`：区域化模式下供 `floating-ball/src/services/llm.ts` 的 `chatFast()` 使用的独立快速模型；未单独配置时回退 `llm.model`
- `llm.enableThinking`：区域化模式下由服务端统一托管的思考模式开关；`floating-ball` 本地只读消费该值，`/v1/ai/chat` 代理转发时会据此决定是否向上游传 `enable_thinking`
- `reviewer.checkExaminationEnabled`：区域化模式下控制是否启用 `check_examination` 独立审查；未显式配置时默认开启，保证旧配置行为不变
- `llm.audioBaseUrl`：服务端实际转发 `/v1/ai/speech/*` 时使用的上游语音转写地址；未单独配置时回退 `llm.baseUrl`
- `llm.audioModel`：服务端实际提交给上游的语音模型；`openai-compatible` 默认 `whisper-1`，`aliyun-dashscope` 默认 `qwen3-asr-flash`
- `speech.provider`：下发给 `floating-ball` 的语音提供方标识，当前兼容 `openai-compatible`、`aliyun-dashscope`
- `speech.model`：下发给 `floating-ball` 的实时语音模型标识；`aliyun-dashscope` 默认 `paraformer-realtime-v2`，也可配置 DashScope `/api-ws/v1/inference` 协议下的 Fun-ASR / Gummy / Paraformer realtime 模型，用于 `/v1/ai/speech/realtime/ws`
- `apiKey`、`audioApiKey` 均不下发给桌面端；区域化模式下主模型和语音上游密钥由 `floating-ball-server` 统一托管

配置优先级：机构级 > 区域级 > 全局级。

生效方式：`/v1/client/bootstrap` 与 `/v1/ai/chat` 每次请求都会按设备当前作用域实时解析 AI 配置；管理端修改配置后，无需重启服务，客户端下一次 `bootstrap` 或 AI 请求即可看到最新结果。

### 3.4 GET `/v1/client/prompts/delta`

请求参数：`version`

响应 `data`：

```json
{
  "version": "2026.04.20.1",
  "prompts": [
    {
      "cdPrompt": "medicalRecordGeneration",
      "sysPrompt": "你是一名全科辅助诊疗助手",
      "userTemplate": "请根据{{chiefComplaint}}生成病历",
      "versionNum": "v1.0"
    }
  ]
}
```

### 3.5 GET `/v1/client/templates/delta`

请求参数：`version`

解析规则：

1. 优先从 `c_ai_symptom_template` 读取当前设备可见范围内已启用的症状模板
2. 合并优先级为机构级 > 区域级 > 全局级；同一 `cdSymptom` 由更高优先级记录覆盖
3. 若症状模板表当前作用域没有任何启用记录，则兼容回退到已发布的 legacy `template` 数据包
4. 若既无症状模板记录，也无 legacy `template` 数据包，则返回服务端内置症状模板基线
5. 当请求 `version` 与服务端当前版本一致时，仅返回最新 `version`，`western/tcm` 可为空数组

响应 `data` 必须兼容 `templateService.ts`：

```json
{
  "version": "2026.04.20.1",
  "western": [],
  "tcm": []
}
```

### 3.6 GET `/v1/client/mappings/delta`

请求参数：`version`

响应 `data` 必须兼容 `medicalData.ts` 当前解析逻辑，返回 CSV 原文字符串：

```json
{
  "version": "2026.04.20.1",
  "diagnoses": "id,code,name,keywords\n1,J00,感冒,咳嗽|流涕",
  "medicines": "id,name,spec\n1,阿莫西林,0.25g*24",
  "items": "id,name,category\n1,血常规,lab_test",
  "tcmDiagnoses": "",
  "tcmSyndromes": "",
  "tcmTreatments": ""
}
```

### 3.7 POST `/v1/client/audit/events/batch`

请求结构以 `auditUploader.ts` 当前实现为准：

```json
{
  "events": [
    {
      "eventId": "uuid",
      "eventType": "operation",
      "payload": {
        "module": "consultation",
        "action": "reference_feedback_diagnosis",
        "title": "接收 PHIS 引用回执",
        "sourceModule": "consultation_reference",
        "scene": "consultation-reference",
        "result": "success",
        "operationType": "api_call",
        "operationName": "reference_feedback:diagnosis",
        "details": {
          "consultationId": "CONSULT-001",
          "traceId": "TRACE-001"
        }
      },
      "timestamp": 1770000000000
    }
  ]
}
```

### 3.8 POST `/v1/client/user-logs/consultations`

用途：桌面端提交运维用户日志快照。该接口独立于原始操作日志，用于按“一名患者一次问诊”聚合首版 AI 生成内容和医生最终修改内容。

请求：

```json
{
  "consultationId": "CONSULT-001",
  "consultationType": "voice",
  "consultationTime": 1770000000000,
  "patientId": "P001",
  "patientName": "王某",
  "patientGender": "男",
  "patientAge": "45岁",
  "doctorId": "D001",
  "doctorName": "张医生",
  "orgCode": "ORG001",
  "orgName": "区域中心医院",
  "deptId": "DEPT001",
  "deptName": "全科",
  "speechText": "医生您好，我发热一天，最高39度，伴咳嗽、咽痛。",
  "audio": "base64-audio",
  "audioMimeType": "audio/wav",
  "audioFormat": "wav",
  "audioFileName": "voice-consultation-1770000000000.wav",
  "firstSnapshot": {
    "chiefComplaint": "咳嗽3天",
    "historyOfPresentIllness": "患者3天前出现咳嗽...",
    "diagnoses": [{ "name": "急性上呼吸道感染", "selected": true }],
    "medicines": [{ "name": "氨溴索", "selected": true }],
    "examinations": [{ "name": "胸部CT", "selected": false }],
    "labTests": [{ "name": "血常规", "selected": true }]
  },
  "finalSnapshot": {
    "chiefComplaint": "咳嗽、咳痰3天",
    "historyOfPresentIllness": "医生修改后的最终现病史...",
    "diagnoses": [{ "name": "急性支气管炎", "selected": true }],
    "medicines": [{ "name": "氨溴索", "selected": true }],
    "examinations": [],
    "labTests": [{ "name": "血常规", "selected": true }]
  },
  "selectionSnapshot": {
    "selectedDiagnosisNames": ["急性支气管炎"],
    "selectedMedicineNames": ["氨溴索"],
    "selectedExaminationNames": [],
    "selectedLabTestNames": ["血常规"]
  }
}
```

约束：

1. `consultationType` 取值：`voice`（语音问诊）、`smart`（智能问诊）。
2. 服务端按 `consultationId + consultationType + idDevice` upsert；先收到首版快照则创建记录，后收到最终快照则更新同一条记录。
3. 客户端不需要上报每一次中间编辑，最终快照只代表医生提交/回写时的最终状态。
4. `speechText` / `audio` 仅用于语音问诊输入复盘；`audio` 为 base64，不带 Data URL 前缀。`audioFormat` 可选，用于在 `audioMimeType` 缺失时辅助推断文件扩展名。服务端把音频落到 `floating-ball.audit.speech-file-dir`，数据库只保存文件路径、MIME、文件名和大小，不把原始 base64 写入快照 JSON。

### 3.9 POST `/v1/client/feedbacks`

用途：桌面端提交问题反馈，统一覆盖**通用反馈（设置入口）**、**语音推荐反馈**、**语音病例字段反馈**、**语音整页评分反馈**四种场景。支持评分、说明、内置截图、问题标签、医生/机构/科室身份回填，以及最近一次 AI 调用链路上下文。

版本语义：当 `chainContext.feedbackScopeKey` 存在时，服务端按“同一设备 + 同一反馈槽位”保留修订历史。新提交会生成一条新记录，并把上一条最新记录标记为历史版本；默认列表与统计仅看最新版本。

请求：

```json
{
  "sessionId": "session-123",
  "traceId": "trace-123",
  "sourceModule": "voice_record_field",
  "kind": "record_field",
  "severity": "medium",
  "score": 3,
  "comment": "主诉漏掉了发热三天",
  "tags": ["data_accuracy", "workflow"],
  "hasCorrection": true,
  "doctorId": "10001",
  "doctorName": "张医生",
  "orgName": "示例社区卫生服务中心",
  "deptId": "DEPT001",
  "deptName": "全科诊疗",
  "screenshot": {
    "fileName": "feedback-2026-04-22.png",
    "mimeType": "image/png",
    "dataUrl": "data:image/png;base64,iVBORw0KGgoAAA..."
  },
  "chainContext": {
    "kind": "record_field",
    "consultationId": "...",
    "aiTrace": { "traceId": "...", "model": "...", "requestSummary": "..." },
    "recordField": { "fieldKey": "chiefComplaint", "originalValue": "...", "currentValue": "...", "correctedValue": "..." }
  }
}
```

字段说明：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `kind` | enum | `general` \| `recommendation` \| `record_field` \| `session`，默认 `general` |
| `severity` | enum | `low` \| `medium` \| `high`，默认 `medium`；通用反馈按评分推导（≤2 high / 3 medium / ≥4 low） |
| `tags` | string[] | 问题标签，最多 20 条，去重并修剪空白；通用反馈使用预置标签 (`recommendation_quality`, `data_accuracy`, `workflow`, `stability`, `ui`, `other`)，语音反馈使用 `issueTags` |
| `hasCorrection` | boolean | 是否包含医生修正（语音 `record_field` / `recommendation` 反馈用） |
| `doctorId` / `doctorName` | string | 医生身份；桌面端从 SDK handshake `urt.idDoctor / naDoctor` 缓存 |
| `orgName` | string | 机构名称；`orgCode` 已经由设备鉴权携带，此处仅补名称 |
| `deptId` / `deptName` | string | 科室身份；从 `urt.userRoleDepts[0]` 解析 |
| `sourceModule` | string | 反馈入口标识。常见取值：`settings_feedback`、`voice_session`、`voice_recommendation`、`voice_record_field` |
| `chainContext.consultationId` | string | 可选，当前问诊锚点；用于把同一问诊的多次反馈修订归到同一槽位 |
| `chainContext.feedbackScopeKey` | string | 可选，反馈槽位唯一键，建议由客户端按“问诊锚点 + 模块”生成 |
| `chainContext.feedbackRevision` | number | 可选，客户端感知到的修订号；服务端会以库内最新版本为准继续递增 |
| `chainContext.previousFeedbackId` | string | 可选，客户端上次提交成功返回的反馈 ID；用于辅助串联修订历史 |

约束：

1. `score` 范围为 `1-5`
2. `comment` 必填（通用反馈若仅勾选标签则前端拼成 `问题标签：xxx` 兜底），建议不超过 2000 字
3. `screenshot` 为可选；若存在，必须是 `data:image/*;base64,...` 形式
4. `traceId` 为可选，但若桌面端能拿到最近一次 AI 代理调用上下文，必须优先回传
5. 设备鉴权携带的 `orgCode` 与请求体 `orgName` 不冲突时同时持久化

响应 `data`：

```json
{
  "feedbackId": "uuid",
  "status": "accepted"
}
```

落表约定：

- `sdLogType` ← `eventType`
- `naModule` ← `payload.module`，若缺失则回退 `operationType / metricType / targetType / sessionType`
- `desOp` ← `payload.action`，若缺失则回退 `operationName / feedbackType / recType`
- `opResult` ← `payload.result`，若缺失则回退 `success`
- `payloadJson` 保留完整原始 payload，供详情查看和兼容后续扩展

版本持久化约定：

- 若 `feedbackScopeKey` 缺失，则按普通单条反馈处理，`fg_latest=1`、`revision_no=1`
- 若 `feedbackScopeKey` 存在，则同一 `id_device + feedback_scope_key` 下仅一条记录 `fg_latest=1`
- 新版本会保留历史记录，并写入 `id_feedback_root`、`previous_feedback_id`、`revision_no`

响应 `data`：

```json
{
  "accepted": 1
}
```

## 4. AI 代理接口

### 4.1 POST `/v1/ai/chat`

用途：OpenAI 兼容聊天代理。

约束：

1. 服务端按设备所属机构 / 区域解析当前生效 AI 配置
2. 当 `stream=true` 时，先完成配置校验，再按 OpenAI 风格 `data: ...` SSE 帧逐段转发上游模型输出，而不是把完整结果一次性封装后再返回
3. 区域化模式下，实际生效的主模型 / 快速模型 / 审查模型与 `enableThinking` 开关以服务端当前配置解析结果为准；客户端不应依赖缓存的 `model` 值覆盖服务端配置
4. 若上游模型服务返回 4xx / 5xx，服务端应尽量提取上游响应体中的可读错误消息，并作为当前接口错误消息返回，避免只暴露 WebClient 堆栈

请求：

```json
{
  "configProfile": "default",
  "model": "deepseek-chat",
  "messages": [
    { "role": "system", "content": "你是医生助手" },
    { "role": "user", "content": "患者咳嗽三天" }
  ],
  "stream": false,
  "temperature": 0.2
}
```

字段说明：

- `configProfile`：可选，默认 `default`
- 当值为 `fast` 时，服务端优先使用当前设备可见 AI 配置中的 `fastModelName`；未配置时回退主模型配置
- 当值为 `reviewer` 时，服务端优先使用当前设备可见 AI 配置中的独立审查模型地址 / 密钥 / 模型；缺失项回退主模型配置
- `enable_thinking` 是否开启由服务端当前 AI 配置统一决定；区域化桌面端不单独透传该开关覆盖服务端配置

非流式响应 `data`：

```json
{
  "content": "建议考虑上呼吸道感染。"
}
```

流式响应：

- `Content-Type: text/event-stream`
- 每帧：`data: {"choices":[{"delta":{"content":"..."}}]}\n\n`
- 结束：`data: [DONE]\n\n`

说明：`floating-ball` 当前 SSE 解析逻辑直接消费 OpenAI 风格 `choices[0].delta.content`。

### 4.2 POST `/v1/ai/speech/transcribe`

请求：

```json
{
  "audio": "base64-audio",
  "mimeType": "audio/webm",
  "fileName": "chat-input-1713686400000.webm",
  "scene": "chat-input"
}
```

字段说明：

- `audio`：必填，`floating-ball` 端录音文件的 base64 内容，不带 Data URL 前缀
- `mimeType`：可选，录音 MIME 类型；服务端会据此构造上游 multipart 文件内容类型
- `fileName`：可选，录音文件名；未传时服务端按 MIME 类型自动补默认扩展名
- `scene`：可选，录音来源场景，如 `chat-input`、`voice-consultation`

服务端处理约束：

1. 先接收 `floating-ball` 上传的 base64 录音
2. 在服务端解码为真实字节数组
3. 若收到 `audio/pcm` / `format=pcm`，服务端会先补 WAV 头再标准化为 `.wav`
4. `speechProvider=openai-compatible` 时，以 `multipart/form-data` 的 `file` 字段转发到上游 `/audio/transcriptions`
5. `speechProvider=aliyun-dashscope` 时，将标准化后的音频组装为 Data URL，调用 DashScope 兼容模式 `/chat/completions`
6. 不把原始 base64 音频直接原样透传给上游 OpenAI 兼容接口

响应 `data`：

```json
{
  "text": "转写结果"
}
```

### 4.3 POST `/v1/ai/speech/realtime`

用途：区域化模式下的实时语音识别批量兜底代理。桌面端实时流式优先使用 `4.3.1` WebSocket 通道；若 WebSocket 不可用，再在录音结束后调用本接口上传整段录音。

### 4.3.1 WebSocket `/v1/ai/speech/realtime/ws`

用途：区域化模式下的 DashScope Paraformer 实时语音识别代理。

连接：

```text
ws(s)://{server}/v1/ai/speech/realtime/ws?token={deviceToken}&clientVersion={version}&updateChannel={channel}&ts={timestamp}&nonce={nonce}&sig={signature}
```

约束：

1. 浏览器 WebSocket 无法设置 `Authorization` 请求头，因此本通道通过 `token` query 参数携带设备令牌；服务端握手阶段按 `DeviceService.findActiveByToken` 校验。
2. `ts / nonce / sig` 为 WebSocket 握手签名参数，签名原文与 HTTP 一致：`GET`、路径 `/v1/ai/speech/realtime/ws`、空 body SHA-256。
3. `clientVersion / updateChannel` 用于握手阶段强制更新门禁；版本过低时拒绝连接。
4. 当前仅在 `speechProvider=aliyun-dashscope` 时启用，服务端使用 `audioApiKey` 或主模型 `apiKey` 连接 DashScope WebSocket。
5. 服务端向 DashScope `/api-ws/v1/inference` 发送 `run-task`，模型取 `speechModel`，默认 `paraformer-realtime-v2`；若配置其他同协议 Fun-ASR / Gummy / Paraformer realtime 模型则原样使用；音频格式为 `pcm` / `16000`。
6. DashScope `qwen3-asr-flash-realtime` 属于另一套 `/api-ws/v1/realtime` session 协议，不属于当前代理通道；若后续要使用该模型，需要新增独立协议适配。

客户端发送：

- 二进制帧：PCM 16k 单声道音频 chunk
- 文本帧：`{"type":"finish"}` 表示结束录音

服务端返回：

```json
{ "type": "text", "text": "患者咳嗽", "isSentenceEnd": false }
```

```json
{ "type": "final", "text": "完整识别文本" }
```

```json
{ "type": "error", "message": "错误说明" }
```

### 4.4 POST `/v1/knowledge/pmphai/search`

用途：区域化模式下的人卫 Inside 智能检索代理。

请求：

```json
{
  "query": "急性上呼吸道感染",
  "type": 1,
  "limit": 5,
  "score": 0.7,
  "enableAbstract": true
}
```

响应 `data`：兼容 `pmphai.ts` 的 `SearchResult[]`。

### 4.5 POST `/v1/knowledge/pmphai/clip`

用途：获取文献详情。

请求：

```json
{
  "id": "000000000000000001"
}
```

响应 `data`：兼容 `pmphai.ts` 的 `ClipData`。

### 4.6 POST `/v1/knowledge/pmphai/list`

用途：区域化模式下的文档浏览 / 传统列表搜索。

请求：

```json
{
  "key": "感冒",
  "kgBaseId": "0001AA100000000EKLSX",
  "pageSize": 10,
  "page": 1,
  "sortField": "initials",
  "sortRule": "asc"
}
```

响应 `data`：兼容 `pmphai.ts` 的 `ListSearchResponse`。

### 4.7 POST `/v1/knowledge/pmphai/page-url`

用途：生成人卫 Inside 页面的签名跳转地址。

请求：

```json
{
  "pageName": "home",
  "kgBaseId": "0001AA100000000EKLSX",
  "id": "000000000000000001",
  "contentId": "content-001",
  "kgFields": "适用性别,用法用量",
  "muluId": "mulu-001",
  "catalogueId": "catalogue-001",
  "originUrl": "https://www.pmphai.com"
}
```

响应 `data`：

```json
{
  "url": "https://pmphai.example.com/aip/oauth/authorize?..."
}
```

### 4.8 GET `/v1/knowledge/pmphai/kgbases`

用途：获取知识库列表。

请求参数：

- `kgBaseId`：可选

### 4.9 GET `/v1/knowledge/pmphai/categories`

用途：获取指定知识库的分类树。

请求参数：

- `kgBaseId`：必填

用途：实时语音代理。

请求：

```json
{
  "audio": "base64-audio",
  "format": "pcm",
  "mimeType": "audio/pcm",
  "fileName": "voice-consultation-1713686400000.pcm",
  "scene": "voice-consultation"
}
```

说明：

1. `floating-ball` 当前在区域化模式下会在语音录制结束后批量上传整段录音，而不是逐帧 WebSocket 透传。
2. 服务端处理方式与 `/v1/ai/speech/transcribe` 一致：先接收 base64 录音，再解码为真实文件后转发给上游语音转写接口。
3. 对 `pcm` 原始录音，服务端会补 WAV 头后再上传，兼容常见 OpenAI 兼容转写服务。
4. `format` 主要用于标记原始采样格式，当前常见值为 `pcm`。

响应 `data`：

```json
{
  "text": "实时识别结果"
}
```

## 5. 管理端接口

> 路径前缀：`/admin/api/*`
>
> 说明：当前包含管理员鉴权、基础治理能力和首期 CRUD 契约。

### 5.1 POST `/admin/api/auth/login`

用途：管理员登录。

请求：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

响应 `data`：

```json
{
  "token": "admin-token",
  "expiresAt": 1770000000000,
  "user": {
    "idUser": "ADMIN001",
    "cdUser": "admin",
    "naUser": "系统管理员",
    "idOrg": "ORG001",
    "roles": ["SYSTEM_ADMIN"]
  }
}
```

### 5.2 GET `/admin/api/auth/me`

用途：返回当前登录管理员信息。

### 5.3 POST `/admin/api/auth/logout`

用途：退出登录；首期可为轻量无状态实现。

### 5.4 GET `/admin/api/stats/overview`

用途：返回管理端首页概览统计。

响应 `data`：

```json
{
  "regionCount": 1,
  "orgCount": 1,
  "deviceCount": 1,
  "configCount": 1,
  "promptCount": 1,
  "dataPackageCount": 1,
  "logCount": 1,
  "userCount": 1,
  "roleCount": 1
}
```

### 5.5 GET `/admin/api/analytics/summary`

用途：返回综合概况统计分析核心指标卡片数据。

请求参数：

- `dateFrom` — 开始日期（yyyy-MM-dd）
- `dateTo` — 结束日期（yyyy-MM-dd）
- `idRegion` — 区域ID（可选）
- `idOrg` — 机构ID（可选）

响应 `data`：

```json
{
  "aiServiceTotal": 12345,
  "avgDailyAiService": 412,
  "aiAdoptionRate": "78.5",
  "diagnosisMatchRate": "65.2",
  "activeDoctorCount": 86,
  "consultationTotal": 2345,
  "aiServiceGrowth": "12.5",
  "avgDailyGrowth": "8.3",
  "adoptionRateGrowth": "-1.2",
  "matchRateGrowth": "3.7",
  "activeDoctorGrowth": "5",
  "consultationGrowth": "15.8"
}
```

指标计算口径：

| 指标 | 分子 | 分母 | 说明 |
|------|------|------|------|
| `aiServiceTotal` | `COUNT(c_ai_op_log WHERE sd_log_type='ai_proxy')` | — | 仅统计服务端 AI 代理调用，不含客户端操作事件 |
| `avgDailyAiService` | `aiServiceTotal` | 查询天数 | |
| `aiAdoptionRate` | `COUNT(status='completed')` | `COUNT(全部问诊)` | 仅"一键回写"计为采纳 |
| `diagnosisMatchRate` | `COUNT(JSON_VALUE(change_summary_json,'$.diagnosisChanges')=0)` | `COUNT(status IN ('completed','abandoned'))` | 比较 AI 最初诊断与医生最终诊断是否一致 |
| `activeDoctorCount` | `COUNT(DISTINCT id_doctor)` | — | |
| `consultationTotal` | `COUNT(全部问诊)` | — | |

### 5.6 GET `/admin/api/analytics/trend`

用途：返回AI服务量与问诊量按日聚合的趋势数据。

请求参数同 5.6。

响应 `data`：

```json
{
  "days": ["2026-05-01", "2026-05-02", "..."],
  "aiServiceValues": [400, 420, 415, "..."],
  "consultationValues": [75, 82, 78, "..."]
}
```

### 5.7 GET `/admin/api/analytics/distribution`

用途：返回机构分布与区域分布数据。

请求参数同 5.6。

响应 `data`：

```json
{
  "orgDistribution": [
    { "orgName": "市第一医院", "value": 600 },
    { "orgName": "市第二医院", "value": 800 }
  ],
  "regionDistribution": [
    { "regionName": "东城区", "value": 4938, "percentage": "40" },
    { "regionName": "西城区", "value": 3704, "percentage": "30" }
  ],
  "totalService": 12345
}
```

### 5.8 GET `/admin/api/analytics/function-modules`

用途：返回所有已记录的功能模块展示名称列表，供辅诊功能应用统计页的功能模块多选下拉使用。

无请求参数。

响应 `data`：

```json
["语音录入", "诊断建议", "知识库查询", "模板推荐", "AI问诊"]
```

### 5.9 GET `/admin/api/analytics/function-usage`

用途：返回辅诊功能应用统计数据，包含汇总指标、功能使用排行、趋势与分页明细。

请求参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `dateFrom` | string | 起始日期 yyyy-MM-dd |
| `dateTo` | string | 截止日期 yyyy-MM-dd |
| `idRegion` | string | 区域 ID（可选） |
| `idOrg` | string | 机构 ID（可选） |
| `functionModules` | string[] | 功能模块筛选（可选，多选），支持传展示名称或原始编码 |
| `current` | int | 当前页（默认 1） |
| `size` | int | 每页条数（默认 20） |

响应 `data`：

```json
{
  "totalCallCount": 52800,
  "avgDailyCalls": 1760,
  "usageRate": "83%",
  "ranking": [
    {
      "moduleName": "语音录入",
      "callCount": 18500,
      "doctorCount": 42,
      "avgPerDoctor": 440,
      "growthRate": "12.5"
    }
  ],
  "total": 6,
  "records": [],
  "trend": {
    "modules": ["语音录入", "诊断建议", "知识库查询"],
    "days": ["2026-04-01", "2026-04-02"],
    "values": [[120, 135], [98, 102], [75, 80]]
  }
}
```

约束：

1. `ranking` 按 `callCount` 倒序排列，已计算增长率（与上一等长周期对比）
2. `moduleName`、`trend.modules` 和 `function-modules` 接口返回的模块名均已统一为后台展示目录中的中文名称，同时保留对原始编码筛选的兼容
2. `trend` 仅包含排名前 5 的功能模块的逐日调用趋势
3. `records` 为当前页数据，支持分页

### 5.10 GET `/admin/api/users`

用途：分页查询用户列表。

请求参数：

- `current`
- `size`
- `keyword`
- `sdStatus`
- `idOrg`
- `idRole`

### 5.11 POST `/admin/api/users`
用途：新增用户。

请求：

```json
{
  "cdUser": "zhangsan",
  "naUser": "张三",
  "password": "123456",
  "idOrg": "ORG001",
  "roleIds": ["ROLE_ADMIN"],
  "sdStatus": "1"
}
```

### 5.12 PUT `/admin/api/users/{idUser}`
用途：修改用户资料、角色和状态；`password` 为空时保留原值。

### 5.13 DELETE `/admin/api/users/{idUser}`
用途：逻辑停用用户。

### 5.14 GET `/admin/api/roles`
用途：分页查询角色列表。

请求参数：

- `current`
- `size`
- `keyword`

### 5.15 POST `/admin/api/roles`
用途：新增角色。

请求：

```json
{
  "cdRole": "SYSTEM_ADMIN",
  "naRole": "系统管理员",
  "desRole": "拥有全部后台权限",
  "sdStatus": "1"
}
```

### 5.16 PUT `/admin/api/roles/{idRole}`
用途：修改角色信息。

### 5.17 DELETE `/admin/api/roles/{idRole}`
用途：逻辑停用角色。

### 5.18 GET `/admin/api/regions`
用途：分页查询区域列表。

请求参数：

- `current`
- `size`
- `keyword`

### 5.19 POST `/admin/api/regions`
用途：新增区域。

请求：

```json
{
  "cdRegion": "REG001",
  "naRegion": "章贡区",
  "idParent": "REG_PARENT",
  "sdRegionType": "district",
  "sdStatus": "1",
  "sortOrder": 10,
  "desRegion": "示例区域"
}
```

### 5.20 PUT `/admin/api/regions/{idRegion}`
用途：修改区域信息。

### 5.21 DELETE `/admin/api/regions/{idRegion}`
用途：逻辑停用区域。

### 5.22 GET `/admin/api/orgs`
用途：分页查询机构列表。

请求参数：

- `current`
- `size`
- `keyword`

### 5.23 POST `/admin/api/orgs`
用途：新增机构。

请求：

```json
{
  "cdOrg": "ORG001",
  "naOrg": "章贡区人民医院",
  "idRegion": "REG001",
  "idParent": "ORG_PARENT",
  "sdOrgType": "community",
  "sdStatus": "1",
  "sortOrder": 10,
  "desOrg": "示例机构"
}
```

### 5.24 PUT `/admin/api/orgs/{idOrg}`
用途：修改机构信息。

### 5.25 DELETE `/admin/api/orgs/{idOrg}`
用途：逻辑停用机构。

### 5.26 GET `/admin/api/devices`
用途：分页查询设备列表。

请求参数：

- `current`
- `size`
- `keyword`

响应 `data`：

```json
{
  "current": 1,
  "size": 10,
  "total": 1,
  "records": [
    {
      "idDevice": "uuid",
      "cdDevice": "9C:4E:36:AA:BB:CC",
      "naDevice": "诊室 1 号机",
      "idOrg": "ORG001",
      "idRegion": "REGION001",
      "deviceTokenMasked": "abcd****wxyz",
      "sdStatus": "1",
      "clientVersion": "0.1.0",
      "osInfo": "Windows 10",
      "dtLastHeartbeat": "2026-04-20T12:00:00",
      "dtRegistered": "2026-04-20T10:00:00"
    }
  ]
}
```

### 5.27 POST `/admin/api/devices`
用途：手工创建设备记录。

请求：

```json
{
  "cdDevice": "9C:4E:36:AA:BB:CC",
  "naDevice": "诊室 1 号机",
  "idOrg": "ORG001",
  "clientVersion": "0.1.0",
  "osInfo": "Windows 10",
  "sdStatus": "1"
}
```

### 5.28 PUT `/admin/api/devices/{idDevice}`
用途：修改设备名称、机构、状态和客户端信息。

### 5.29 DELETE `/admin/api/devices/{idDevice}`
用途：逻辑停用设备。

### 5.30 GET `/admin/api/configs`
用途：分页查询 AI 配置列表。

### 5.31 POST `/admin/api/configs`
用途：新增 AI 配置。

请求：

```json
{
  "cdConfig": "default-llm",
  "naConfig": "默认模型配置",
  "provider": "deepseek",
  "apiBaseUrl": "https://example.com/v1",
  "apiKey": "sk-xxx",
  "modelName": "deepseek-chat",
  "fastModelName": "deepseek-chat-lite",
  "enableThinking": true,
  "audioBaseUrl": "https://example.com/v1",
  "audioApiKey": "sk-audio",
  "audioModel": "whisper-1",
  "speechProvider": "openai-compatible",
  "speechModel": "whisper-1",
  "knowledgeBaseEnabled": true,
  "knowledgeBaseBaseUrl": "https://pmphai.example.com",
  "pmphaiEnabled": true,
  "pmphaiBaseUrl": "https://pmphai.example.com",
  "pmphaiAppKey": "pmph-app-key",
  "pmphaiAppSecret": "pmph-app-secret",
  "reviewerEnabled": false,
  "reviewerBaseUrl": "https://reviewer.example.com/v1",
  "reviewerApiKey": "sk-reviewer",
  "reviewerModel": "gpt-4o-mini",
  "reviewerCheckExaminationEnabled": true,
  "featuresJson": "{\"regionalMode\":true,\"aiProxyEnabled\":true}",
  "idOrg": "ORG001",
  "idRegion": "REGION001",
  "sdStatus": "1"
}
```

语音配置最小要求：

0. `fastModelName` 为区域化 `chatFast` 独立模型；可留空，留空时回退 `modelName`。
1. `enableThinking` 用于控制服务端代理主模型 / `chatFast` / 审查模型时是否附带上游 `enable_thinking`；默认关闭。
2. `apiKey` 为主模型密钥，也是 `audioApiKey` 留空时的语音密钥回退值。
3. `audioApiKey` 为语音上游独立密钥；可留空，留空时复用 `apiKey`，当语音和主模型供应商不一致时必须填写。
4. `audioBaseUrl` 为服务端实际语音转写地址；可留空，留空时复用 `apiBaseUrl`。
5. `audioModel` 为服务端实际转写模型；可留空，`openai-compatible` 默认 `whisper-1`，`aliyun-dashscope` 默认 `qwen3-asr-flash`。
6. `speechProvider` / `speechModel` 用于 `/v1/client/bootstrap` 下发给桌面端；`speechProvider=aliyun-dashscope` 时，`speechModel` 同时作为服务端实时 WebSocket 代理连接 DashScope `/api-ws/v1/inference` 的模型名，默认 `paraformer-realtime-v2`，也可填同协议 Fun-ASR / Gummy / Paraformer realtime 模型名。

语音上游路径：

1. `speechProvider=openai-compatible`：服务端将录音文件转为 multipart，调用 `{audioBaseUrl}/audio/transcriptions`。
2. `speechProvider=aliyun-dashscope`：服务端将录音转为 Data URL，调用 DashScope 兼容模式 `{audioBaseUrl}/chat/completions`；`audioBaseUrl` 建议配置为 `https://dashscope.aliyuncs.com/compatible-mode/v1`。

### 5.32 PUT `/admin/api/configs/{idConfig}`
用途：修改 AI 配置；`apiKey`、`audioApiKey`、`reviewerApiKey`、`pmphaiAppKey`、`pmphaiAppSecret` 为空时保留原值。

### 5.33 DELETE `/admin/api/configs/{idConfig}`
用途：逻辑停用配置。

### 5.34 GET `/admin/api/prompts`
用途：分页查询 Prompt 列表。

### 5.35 POST `/admin/api/prompts`
用途：新增 Prompt。

请求：

```json
{
  "cdPrompt": "medicalRecordGeneration",
  "naPrompt": "病历生成",
  "sysPrompt": "你是一名全科辅助诊疗助手",
  "userTemplate": "请根据{{chiefComplaint}}生成病历",
  "versionNum": "2026.04.20.1",
  "sdPromptType": "consultation",
  "sdStatus": "0",
  "idOrg": "ORG001",
  "idRegion": "REGION001"
}
```

### 5.36 PUT `/admin/api/prompts/{idPrompt}`
用途：修改 Prompt 内容与可见范围。

### 5.37 POST `/admin/api/prompts/{idPrompt}/publish`
用途：发布 Prompt；同场景下其他已发布版本自动转归归档态。

### 5.38 POST `/admin/api/prompts/{idPrompt}/archive`
用途：归档 Prompt。

### 5.39 DELETE `/admin/api/prompts/{idPrompt}`
用途：逻辑删除 Prompt。

### 5.40 GET `/admin/api/data-packages`
用途：分页查询数据包列表。

请求参数：

- `current`
- `size`
- `keyword`
- `sdPackageType`
- `sdStatus`
- `idRegion`
- `idOrg`

### 5.41 POST `/admin/api/data-packages`
用途：新增数据包。

请求：

```json
{
  "cdPackage": "mapping-default",
  "naPackage": "默认映射包",
  "sdPackageType": "mapping",
  "versionNum": "2026.04.20.1",
  "contentJson": "{\"diagnoses\":\"id,code,name\\n1,J00,感冒\"}",
  "sdStatus": "0",
  "idOrg": "ORG001",
  "idRegion": "REGION001"
}
```

### 5.42 PUT `/admin/api/data-packages/{idPackage}`
用途：修改数据包内容、版本和可见范围。

### 5.43 POST `/admin/api/data-packages/{idPackage}/publish`
用途：发布数据包。

约束：

- 同类型、同作用域的其他已发布版本需自动转归归档态

### 5.44 POST `/admin/api/data-packages/{idPackage}/archive`
用途：归档数据包。

### 5.45 DELETE `/admin/api/data-packages/{idPackage}`
用途：逻辑删除数据包。

### 5.46 GET `/admin/api/data-packages/template-default`
用途：获取服务端内置症状模板基线，供 legacy `template` 数据包编辑或症状模板初始化导入时参考。

响应 `data`：

```json
{
  "version": "builtin-1a2b3c4d",
  "western": [],
  "tcm": []
}
```

### 5.47 GET `/admin/api/symptom-templates`
用途：分页查询症状模板列表，返回完整模板结构，供后台 disease editor 直接编辑。

请求参数：

- `current`
- `size`
- `keyword`
- `medicalMode`
- `systemCategory`
- `sdStatus`
- `idRegion`
- `idOrg`

响应 `data.records[*]` 结构：

```json
{
  "id": "uuid",
  "medicalMode": "western",
  "key": "fever",
  "name": "发热",
  "description": "",
  "isCommonSymptom": true,
  "systemCategory": [
    "nervous"
  ],
  "bodyParts": [],
  "customScript": "",
  "config": {
    "title": "智能问诊",
    "sections": []
  },
  "applicablePopulation": {
    "genders": [],
    "ageGroups": []
  },
  "tcmMetadata": null,
  "sortOrder": 10,
  "sdStatus": "1",
  "idRegion": null,
  "idOrg": null,
  "createdAt": 1770000000000,
  "updatedAt": 1770000000000
}
```

### 5.48 POST `/admin/api/symptom-templates`
用途：新增症状模板。

说明：

- 请求体使用与桌面端 disease editor 基本一致的模板结构
- `medicalMode` 仅支持 `western`、`tcm`

### 5.49 PUT `/admin/api/symptom-templates/{id}`
用途：修改症状模板。

### 5.50 DELETE `/admin/api/symptom-templates/{id}`
用途：逻辑删除症状模板。

### 5.51 POST `/admin/api/symptom-templates/import-builtin`
用途：将服务端内置 `template-seeds` 症状模板按指定作用域导入 `c_ai_symptom_template`。

请求：

```json
{
  "medicalMode": "western",
  "idRegion": null,
  "idOrg": null,
  "overwriteExisting": true
}
```

响应 `data`：

```json
{
  "medicalMode": "western",
  "createdCount": 265,
  "updatedCount": 0
}
```

### 5.52 POST `/admin/api/symptom-templates/import-json`
用途：将桌面端已有症状模板 JSON 文件写入 `c_ai_symptom_template`，支持 `templates.json`、`tcm-templates.json` 以及后台导出的当前模式症状数组。

请求：

```json
{
  "medicalMode": "western",
  "idRegion": null,
  "idOrg": null,
  "overwriteExisting": true,
  "contentJson": "[{\"key\":\"fever\",\"name\":\"发热\",\"config\":{\"sections\":[]}}]"
}
```

说明：

- 当 `contentJson` 为 JSON 数组时，按 `medicalMode` 解释为当前模式的症状列表
- 当 `contentJson` 为对象且包含 `symptoms` 数组时，按 `medicalMode` 读取其中症状列表，兼容 `tcm-templates.json`
- 当 `contentJson` 为对象且包含 `western` 或 `tcm` 数组时，按 `medicalMode` 读取对应字段

响应 `data`：

```json
{
  "medicalMode": "western",
  "createdCount": 12,
  "updatedCount": 3
}
```

### 5.53 GET `/admin/api/logs`
用途：分页查询操作日志。

请求参数：

- `current`
- `size`
- `keyword`
- `logType`
- `module`
- `action`
- `title`
- `sourceModule`
- `scene`
- `traceId`
- `consultationId`
- `result`
- `dateFrom`
- `dateTo`

响应 `data`：

```json
{
  "current": 1,
  "size": 10,
  "total": 1,
  "records": [
    {
      "idLog": "uuid",
      "idDevice": "uuid",
      "idOrg": "ORG001",
      "sdLogType": "operation",
      "naModule": "consultation",
      "displayModule": "智能问诊",
      "opAction": "reference_feedback_diagnosis",
      "displayAction": "接收诊断引用回执",
      "opTitle": "接收 PHIS 引用回执",
      "displayTitle": "接收 PHIS 诊断引用回执",
      "sourceModule": "consultation_reference",
      "displaySourceModule": "问诊引用",
      "sceneCode": "consultation-reference",
      "displayScene": "问诊引用回写",
      "traceId": "TRACE-001",
      "consultationId": "CONSULT-001",
      "desOp": "接收 PHIS 引用回执",
      "opResult": "1",
      "payloadJson": "{\"module\":\"consultation\",\"action\":\"reference_feedback_diagnosis\",\"title\":\"接收 PHIS 引用回执\",\"sourceModule\":\"consultation_reference\",\"scene\":\"consultation-reference\",\"result\":\"success\",\"operationType\":\"api_call\",\"operationName\":\"reference_feedback:diagnosis\",\"details\":{\"consultationId\":\"CONSULT-001\",\"traceId\":\"TRACE-001\"}}",
      "audioFilePath": "/var/lib/floating-ball-server/speech-audit/20260422/chat-input-abc123.wav",
      "operationTime": "2026-04-20T16:00:00"
    }
  ]
}
```

补充约束：

1. `speech_proxy` 日志的 `payloadJson` 只保留录音元数据、请求摘要和上游回文，不再保存原始 base64 音频
2. 若存在录音文件落盘，返回记录中的 `audioFilePath` 会指向该文件
3. `desOp` 默认优先展示 `title`，缺失时回退 `action / operationName`
4. `traceId` 默认从顶层 `traceId` 提取；若顶层缺失则回退 `details.traceId`
5. `displayModule/displayAction/displayTitle/displaySourceModule/displayScene` 为服务端统一生成的中文展示字段；管理端应优先展示这些字段，同时保留 `naModule/opAction/opTitle/sourceModule/sceneCode` 原始码用于精准排障与复制检索

### 5.54 GET `/admin/api/user-logs/consultations`
用途：分页查询运维用户日志列表。该模块是专门给运维人员使用的问诊聚合日志，不替代原有操作日志页面。

请求参数：

- `current`、`size`
- `keyword`：跨机构、医生、患者、问诊 ID 模糊搜索
- `consultationType`：`voice` / `smart`
- `dateFrom`、`dateTo`

响应 `data`：

```json
{
  "current": 1,
  "size": 10,
  "total": 1,
  "records": [
    {
      "idLog": "uuid",
      "consultationId": "CONSULT-001",
      "naOrg": "区域中心医院",
      "naDoctor": "张医生",
      "consultationTime": "2026-04-27T10:00:00",
      "patientName": "王某",
      "patientGender": "男",
      "patientAge": "45岁",
      "consultationType": "voice",
      "hasAudio": true,
      "hasSpeechText": true,
      "status": "completed"
    }
  ]
}
```

### 5.55 GET `/admin/api/user-logs/consultations/{idLog}`
用途：查看一次问诊的运维用户日志详情。

响应 `data`：

```json
{
  "idLog": "uuid",
  "consultationId": "CONSULT-001",
  "idOrg": "ORG001",
  "naOrg": "区域中心医院",
  "idDoctor": "D001",
  "naDoctor": "张医生",
  "patientName": "王某",
  "consultationType": "voice",
  "speechText": "医生您好，我发热一天，最高39度，伴咳嗽、咽痛。",
  "audioFileName": "voice-consultation-1770000000000.wav",
  "audioMimeType": "audio/wav",
  "audioSize": 128000,
  "hasAudio": true,
  "hasSpeechText": true,
  "firstSnapshotJson": "{\"chiefComplaint\":\"咳嗽3天\"}",
  "finalSnapshotJson": "{\"chiefComplaint\":\"咳嗽、咳痰3天\"}",
  "selectionJson": "{\"selectedMedicineNames\":[\"氨溴索\"]}",
  "status": "completed",
  "consultationTime": "2026-04-27T10:00:00"
}
```

### 5.53.1 GET `/admin/api/user-logs/consultations/{idLog}/audio`
用途：后台用户日志详情播放语音问诊原始录音。

响应：

- `Content-Type`：优先使用日志中的 `audioMimeType`，缺失时为 `application/octet-stream`
- `Content-Disposition: inline`
- Body：录音文件二进制内容

约束：

1. 仅管理端鉴权后可访问。
2. 接口只根据 `idLog` 查表后读取服务端保存的音频文件，不接受任意文件路径参数。

### 5.56 GET `/admin/api/user-logs/consultations/{idLog}/timeline`
用途：查看一次问诊关联的操作时间线，按发生时间正序返回，便于快速识别关键步骤。

响应 `data`：

```json
[
  {
    "eventType": "operation",
    "module": "consultation",
    "displayModule": "智能问诊",
    "action": "完成智能问诊",
    "displayAction": "完成智能问诊",
    "result": "1",
    "operationTime": "2026-04-27T10:05:00",
    "details": {}
  }
]
```

补充约束：

1. `module/action` 保留原始入库值；`displayModule/displayAction` 为服务端统一生成的中文展示字段。
2. 管理端时间线应优先展示 `displayModule/displayAction`，同时保留原始字段用于精确定位与排障。

### 5.51 GET `/admin/api/feedbacks`

用途：分页查询用户反馈列表。摘要字段面向非技术运营人员，技术列在管理端"高级筛选"中按需展开。默认只返回每个反馈槽位的最新版本；如需查看历史修订，可显式传 `includeHistory=true`。

请求参数：

- `current`、`size`
- `includeHistory`：是否包含历史修订，默认 `false`
- `keyword`：跨 `comment / sourceModule / traceId / sessionId / na_org / na_doctor / na_dept` 模糊搜索
- `score`、`sourceModule`、`kind`（`general | recommendation | record_field | session`）、`severity`（`low | medium | high`）
- `doctor`：医生模糊（先按 `id_doctor` 精确，再 `na_doctor` 模糊）
- `dept`：科室模糊（同上）
- `org`：机构模糊（按 `id_org` 精确 + `na_org` 模糊）
- `hasCorrection`、`hasTrace`：布尔过滤
- `dateFrom`、`dateTo`

响应 `data.records[*]` 重点字段：

```json
{
  "feedbackId": "uuid",
  "score": 2,
  "kind": "record_field",
  "severity": "medium",
  "comment": "主诉漏掉发热三天",
  "sourceModule": "voice_record_field",
  "displaySourceModule": "语音病例字段",
  "tags": ["data_accuracy"],
  "hasCorrection": true,
  "hasTrace": true,
  "hasScreenshot": false,
  "idDoctor": "10001",
  "naDoctor": "张医生",
  "idDept": "DEPT001",
  "naDept": "全科诊疗",
  "idOrg": "ORG-DEMO",
  "naOrg": "示例社区卫生服务中心",
  "traceId": "trace-123",
  "sessionId": "session-123",
  "idDevice": "device-uuid",
  "revisionNo": 2,
  "latest": true,
  "createdAt": "2026-04-22T10:11:12"
}
```

补充约束：

1. `sourceModule` 仍保留原始编码；`displaySourceModule` 为服务端统一生成的中文展示字段，管理端列表与详情应优先展示该字段。
2. `sourceModule` 查询兼容中文展示名和原始编码，两者都可命中同一批反馈记录。

### 5.57 GET `/admin/api/feedbacks/{feedbackId}`
用途：查看反馈详情。管理端将其拆分为「摘要」与「技术详情」两个 Tab，前者展示评分/说明/医生身份/标签/截图，后者展示 traceId/chainContext/sessionId 等技术字段，便于工程师排查。

响应 `data`：

```json
{
  "feedback": {
    "feedbackId": "uuid",
    "score": 2,
    "kind": "record_field",
    "severity": "medium",
    "comment": "主诉漏掉发热三天",
    "sourceModule": "voice_record_field",
    "displaySourceModule": "语音病例字段",
    "tags": ["data_accuracy"],
    "hasCorrection": true,
    "hasTrace": true,
    "hasScreenshot": false,
    "idDoctor": "10001",
    "naDoctor": "张医生",
    "idDept": "DEPT001",
    "naDept": "全科诊疗",
    "idOrg": "ORG-DEMO",
    "naOrg": "示例社区卫生服务中心",
    "traceId": "trace-123",
    "sessionId": "session-123",
    "revisionNo": 2,
    "latest": true,
    "rootFeedbackId": "uuid-root",
    "previousFeedbackId": "uuid-prev",
    "screenshotDataUrl": null,
    "chainContext": {
      "kind": "record_field",
      "aiTrace": { "traceId": "trace-123", "model": "deepseek-chat" },
      "recordField": { "fieldKey": "chiefComplaint", "originalValue": "...", "currentValue": "...", "correctedValue": "..." }
    }
  },
  "timeline": [
    { "type": "ai_proxy", "time": "2026-04-22T10:11:10", "title": "AI 对话请求", "displaySourceModule": "AI 对话代理", "result": "success", "payload": {} },
    { "type": "feedback", "time": "2026-04-22T10:11:12", "title": "用户提交反馈", "displaySourceModule": "语音病例字段", "result": "success", "payload": {} }
  ]
}
```

补充约束：

1. `timeline[*].title` 已按服务端统一展示目录优先中文化；管理端无需再自行猜测 `payload.action/operationName`。
2. `timeline[*].displaySourceModule` 仅用于快速识别来源模块，排障仍应结合 `payload` 原始字段。

## 5. 管理端接口范围

首期管理端 API 只要求覆盖以下资源：

1. 区域管理
2. 机构管理
3. 设备管理
4. AI 配置
5. Prompt 管理
6. 数据包管理
7. 操作日志查询
8. 管理员登录
9. 用户管理
10. 角色管理
11. 概览统计

这些接口在第一轮脚手架阶段优先保证 CRUD 结构和分页查询能力，细节以实现文档和代码为准。

### 5.58 GET `/admin/api/user-activity/summary`

用途：返回指定时间范围和区域下的用户活跃度汇总指标。

鉴权：`Authorization: Bearer {adminToken}`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| dateFrom | string | 否 | 起始日期，yyyy-MM-dd |
| dateTo | string | 否 | 截止日期，yyyy-MM-dd |
| idRegion | string | 否 | 区域 ID，为空时统计全部 |
| timeRange | string | 否 | 时间范围：month / lastMonth / custom |

响应 `data`：

```json
{
  "activeUsers": 128,
  "inactiveUsers": 16,
  "activityRate": "88.9",
  "avgUsageDuration": "2.5 小时",
  "activeUsersGrowth": "8.3",
  "inactiveUsersGrowth": "1",
  "activityRateGrowth": "2.5",
  "avgUsageDurationGrowth": "0.2 小时"
}
```

字段说明：

- `activeUsers`：所选时段内有问诊记录的设备数
- `inactiveUsers`：所选时段内无问诊记录的设备数
- `activityRate`：活跃率百分比，活跃用户数 / 总设备数 × 100
- `avgUsageDuration`：活跃用户平均使用时长估算
- `*Growth`：较上期增长率或变化量

### 5.59 GET `/admin/api/user-activity/region-tree`

用途：返回区域层级树，每个节点包含该区域下的活跃用户数。

鉴权：`Authorization: Bearer {adminToken}`

请求参数：同 `summary` 接口。

响应 `data`：

```json
[
  {
    "id": "REGION001",
    "name": "北京市",
    "type": "province",
    "userCount": 16,
    "children": [
      {
        "id": "REGION002",
        "name": "北京市",
        "type": "city",
        "userCount": 16,
        "children": [
          {
            "id": "REGION003",
            "name": "东城区",
            "type": "district",
            "userCount": 5,
            "children": []
          }
        ]
      }
    ]
  }
]
```

### 5.60 GET `/admin/api/user-activity/users`

用途：返回用户活跃度明细列表，支持按活跃状态筛选和分页。

鉴权：`Authorization: Bearer {adminToken}`

请求参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| dateFrom | string | 否 | 起始日期 |
| dateTo | string | 否 | 截止日期 |
| idRegion | string | 否 | 区域 ID |
| timeRange | string | 否 | 时间范围 |
| activeStatus | string | 否 | 活跃状态筛选：active / inactive |
| current | long | 否 | 页码，默认 1 |
| size | long | 否 | 每页条数，默认 10 |

响应 `data`：

```json
{
  "current": 1,
  "size": 10,
  "total": 144,
  "records": [
    {
      "idDevice": "uuid",
      "cdDevice": "9C:4E:36:AA:BB:CC",
      "naDevice": "FloatingBall-win32",
      "idOrg": "ORG001",
      "naOrg": "区域中心医院",
      "idRegion": "REGION003",
      "naRegion": "东城区",
      "activeStatus": "active",
      "consultationCount": 12,
      "operationCount": 45,
      "lastActiveTime": "2026-05-15T10:30:00"
    }
  ]
}
```
