# floating-ball-server API 说明

> 更新日期：2026-04-23
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

## 2. 认证

### 2.1 客户端接口

- 路径前缀：`/v1/*`
- 认证方式：`Authorization: Bearer {deviceToken}`
- 例外：`POST /v1/client/register` 无需认证

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

## 3. 客户端接口

### 3.1 POST `/v1/client/register`

用途：客户端首次启动时注册设备。

请求：

```json
{
  "cdDevice": "9C:4E:36:AA:BB:CC",
  "naDevice": "FloatingBall-win32",
  "cdOrg": "ORG001",
  "clientVersion": "0.1.0",
  "osInfo": "Windows 10"
}
```

说明：当前桌面端优先使用设备 MAC 地址作为 `cdDevice`；仅在当前环境无法读取 MAC 时才回退到本地兜底编码。

响应 `data`：

```json
{
  "idDevice": "uuid",
  "deviceToken": "32-char-token",
  "heartbeatInterval": 30
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
    "audioBaseUrl": "https://example.com/v1",
    "audioModel": "whisper-1"
  },
  "speech": {
    "provider": "aliyun",
    "model": "paraformer-realtime"
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
    "model": "gpt-4o-mini"
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

配置优先级：机构级 > 区域级 > 全局级。

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
        "module": "api_call",
        "action": "reference_feedback:diagnosis",
        "result": "success",
        "operationType": "api_call",
        "operationName": "reference_feedback:diagnosis",
        "details": {
          "consultationId": "CONSULT-001"
        }
      },
      "timestamp": 1770000000000
    }
  ]
}
```

### 3.8 POST `/v1/client/feedbacks`

用途：桌面端提交问题反馈，支持评分、说明、内置截图结果/上传图片，以及最近一次 AI 调用链路上下文。

请求：

```json
{
  "sessionId": "session-123",
  "traceId": "trace-123",
  "sourceModule": "settings_feedback",
  "score": 2,
  "comment": "语音转写后诊断建议为空白，请排查。",
  "screenshot": {
    "fileName": "feedback-2026-04-22.png",
    "mimeType": "image/png",
    "dataUrl": "data:image/png;base64,iVBORw0KGgoAAA..."
  },
  "chainContext": {
    "channel": "chat",
    "scene": "chat-panel",
    "configProfile": "default",
    "model": "deepseek-chat",
    "requestSummary": "1 条用户问题，约 48 字",
    "responseSummary": "返回为空白回复",
    "startedAt": 1770000000000,
    "finishedAt": 1770000001523,
    "durationMs": 1523,
    "success": false
  }
}
```

约束：

1. `score` 范围为 `1-5`
2. `comment` 必填，建议不超过 2000 字
3. `screenshot` 为可选；若存在，必须是 `data:image/*;base64,...` 形式
4. `traceId` 为可选，但若桌面端能拿到最近一次 AI 代理调用上下文，必须优先回传

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
3. 区域化模式下，实际生效的主模型 / 审查模型以服务端当前配置解析结果为准；客户端不应依赖缓存的 `model` 值覆盖服务端配置
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
- 当值为 `reviewer` 时，服务端优先使用当前设备可见 AI 配置中的独立审查模型地址 / 密钥 / 模型；缺失项回退主模型配置

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
4. 以 `multipart/form-data` 的 `file` 字段转发到上游 `/audio/transcriptions`
5. 不把原始 base64 音频直接原样透传给上游 OpenAI 兼容接口

响应 `data`：

```json
{
  "text": "转写结果"
}
```

### 4.3 POST `/v1/ai/speech/realtime`

用途：区域化模式下的实时语音识别代理。

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

### 5.5 GET `/admin/api/users`

用途：分页查询用户列表。

请求参数：

- `current`
- `size`
- `keyword`
- `sdStatus`
- `idOrg`
- `idRole`

### 5.6 POST `/admin/api/users`

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

### 5.7 PUT `/admin/api/users/{idUser}`

用途：修改用户资料、角色和状态；`password` 为空时保留原值。

### 5.8 DELETE `/admin/api/users/{idUser}`

用途：逻辑停用用户。

### 5.9 GET `/admin/api/roles`

用途：分页查询角色列表。

请求参数：

- `current`
- `size`
- `keyword`

### 5.10 POST `/admin/api/roles`

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

### 5.11 PUT `/admin/api/roles/{idRole}`

用途：修改角色信息。

### 5.12 DELETE `/admin/api/roles/{idRole}`

用途：逻辑停用角色。

### 5.13 GET `/admin/api/regions`

用途：分页查询区域列表。

请求参数：

- `current`
- `size`
- `keyword`

### 5.14 POST `/admin/api/regions`

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

### 5.15 PUT `/admin/api/regions/{idRegion}`

用途：修改区域信息。

### 5.16 DELETE `/admin/api/regions/{idRegion}`

用途：逻辑停用区域。

### 5.17 GET `/admin/api/orgs`

用途：分页查询机构列表。

请求参数：

- `current`
- `size`
- `keyword`

### 5.18 POST `/admin/api/orgs`

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

### 5.19 PUT `/admin/api/orgs/{idOrg}`

用途：修改机构信息。

### 5.20 DELETE `/admin/api/orgs/{idOrg}`

用途：逻辑停用机构。

### 5.21 GET `/admin/api/devices`

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

### 5.22 POST `/admin/api/devices`

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

### 5.23 PUT `/admin/api/devices/{idDevice}`

用途：修改设备名称、机构、状态和客户端信息。

### 5.24 DELETE `/admin/api/devices/{idDevice}`

用途：逻辑停用设备。

### 5.25 GET `/admin/api/configs`

用途：分页查询 AI 配置列表。

### 5.26 POST `/admin/api/configs`

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
  "audioBaseUrl": "https://example.com/v1",
  "audioModel": "whisper-1",
  "speechProvider": "aliyun",
  "speechModel": "paraformer-realtime",
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
  "featuresJson": "{\"regionalMode\":true,\"aiProxyEnabled\":true}",
  "idOrg": "ORG001",
  "idRegion": "REGION001",
  "sdStatus": "1"
}
```

### 5.27 PUT `/admin/api/configs/{idConfig}`

用途：修改 AI 配置；`apiKey`、`reviewerApiKey`、`pmphaiAppKey`、`pmphaiAppSecret` 为空时保留原值。

### 5.28 DELETE `/admin/api/configs/{idConfig}`

用途：逻辑停用配置。

### 5.29 GET `/admin/api/prompts`

用途：分页查询 Prompt 列表。

### 5.30 POST `/admin/api/prompts`

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

### 5.31 PUT `/admin/api/prompts/{idPrompt}`

用途：修改 Prompt 内容与可见范围。

### 5.32 POST `/admin/api/prompts/{idPrompt}/publish`

用途：发布 Prompt；同场景下其他已发布版本自动转归归档态。

### 5.33 POST `/admin/api/prompts/{idPrompt}/archive`

用途：归档 Prompt。

### 5.34 DELETE `/admin/api/prompts/{idPrompt}`

用途：逻辑删除 Prompt。

### 5.35 GET `/admin/api/data-packages`

用途：分页查询数据包列表。

请求参数：

- `current`
- `size`
- `keyword`
- `sdPackageType`
- `sdStatus`
- `idRegion`
- `idOrg`

### 5.36 POST `/admin/api/data-packages`

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

### 5.37 PUT `/admin/api/data-packages/{idPackage}`

用途：修改数据包内容、版本和可见范围。

### 5.38 POST `/admin/api/data-packages/{idPackage}/publish`

用途：发布数据包。

约束：

- 同类型、同作用域的其他已发布版本需自动转归归档态

### 5.39 POST `/admin/api/data-packages/{idPackage}/archive`

用途：归档数据包。

### 5.40 DELETE `/admin/api/data-packages/{idPackage}`

用途：逻辑删除数据包。

### 5.41 GET `/admin/api/data-packages/template-default`

用途：获取服务端内置症状模板基线，供 legacy `template` 数据包编辑或症状模板初始化导入时参考。

响应 `data`：

```json
{
  "version": "builtin-1a2b3c4d",
  "western": [],
  "tcm": []
}
```

### 5.42 GET `/admin/api/symptom-templates`

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

### 5.43 POST `/admin/api/symptom-templates`

用途：新增症状模板。

说明：

- 请求体使用与桌面端 disease editor 基本一致的模板结构
- `medicalMode` 仅支持 `western`、`tcm`

### 5.44 PUT `/admin/api/symptom-templates/{id}`

用途：修改症状模板。

### 5.45 DELETE `/admin/api/symptom-templates/{id}`

用途：逻辑删除症状模板。

### 5.46 POST `/admin/api/symptom-templates/import-builtin`

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

### 5.47 POST `/admin/api/symptom-templates/import-json`

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

### 5.48 GET `/admin/api/logs`

用途：分页查询操作日志。

请求参数：

- `current`
- `size`
- `keyword`
- `logType`
- `module`
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
      "naModule": "api_call",
      "desOp": "reference_feedback:diagnosis",
      "opResult": "1",
      "payloadJson": "{\"module\":\"api_call\",\"action\":\"reference_feedback:diagnosis\",\"result\":\"success\",\"operationType\":\"api_call\",\"operationName\":\"reference_feedback:diagnosis\",\"details\":{\"consultationId\":\"CONSULT-001\"}}",
      "audioFilePath": "/var/lib/floating-ball-server/speech-audit/20260422/chat-input-abc123.wav",
      "operationTime": "2026-04-20T16:00:00"
    }
  ]
}
```

补充约束：

1. `speech_proxy` 日志的 `payloadJson` 只保留录音元数据、请求摘要和上游回文，不再保存原始 base64 音频
2. 若存在录音文件落盘，返回记录中的 `audioFilePath` 会指向该文件

### 5.49 GET `/admin/api/feedbacks`

用途：分页查询用户反馈列表。

请求参数：

- `current`
- `size`
- `keyword`
- `score`
- `sourceModule`
- `dateFrom`
- `dateTo`

响应 `data.records[*]` 重点字段：

```json
{
  "feedbackId": "uuid",
  "score": 2,
  "comment": "语音转写后诊断建议为空白，请排查。",
  "sourceModule": "settings_feedback",
  "traceId": "trace-123",
  "sessionId": "session-123",
  "hasScreenshot": true,
  "createdAt": "2026-04-22T10:11:12"
}
```

### 5.50 GET `/admin/api/feedbacks/{feedbackId}`

用途：查看反馈详情及调用链路时间线。

响应 `data`：

```json
{
  "feedback": {
    "feedbackId": "uuid",
    "score": 2,
    "comment": "语音转写后诊断建议为空白，请排查。",
    "sourceModule": "settings_feedback",
    "traceId": "trace-123",
    "sessionId": "session-123",
    "screenshotDataUrl": "data:image/png;base64,...",
    "chainContext": {
      "channel": "chat",
      "scene": "chat-panel",
      "requestSummary": "1 条用户问题，约 48 字",
      "responseSummary": "返回为空白回复"
    }
  },
  "timeline": [
    {
      "type": "ai_proxy",
      "time": "2026-04-22T10:11:10",
      "title": "chat",
      "result": "success",
      "payload": {}
    },
    {
      "type": "feedback",
      "time": "2026-04-22T10:11:12",
      "title": "用户提交反馈",
      "result": "success",
      "payload": {}
    }
  ]
}
```

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
