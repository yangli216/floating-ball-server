# floating-ball-server 架构说明

> 更新日期：2026-04-23

## 1. 项目定位

`floating-ball-server` 是 `floating-ball` 的配套后台，承担两类职责：

1. 面向桌面端的远端客户端能力：设备注册、配置引导、数据包增量下发、AI 代理、审计上报
2. 面向管理员的后台管理能力：区域、机构、设备、AI 配置、Prompt、数据包、日志
3. 面向平台管理员的基础治理能力：管理员登录、用户、角色、概览统计

本项目不承接 `floating-ball` 的本地 HIS 桥接，不替代 `floating-ball/api.md` 中的 `/api/consultation/*`。

## 2. 技术基线

### 2.1 后端

- Java 8
- Spring Boot 2.7.x
- MyBatis-Plus
- Oracle 19c
- Maven
- WebClient / RestTemplate 用于上游 AI 与语音代理

### 2.2 管理端

- Vue 2
- Element UI
- Axios
- Vue Router
- npm
- 与 Spring Boot 同仓构建、同进程发布

## 3. 目录规划

```text
floating-ball-server/
├── AGENTS.md
├── ARCHITECTURE.md
├── API.md
└── server/
    ├── pom.xml
    └── src/
        ├── main/java/com/regionalai/floatingball/server/
        │   ├── FloatingBallServerApplication.java
        │   ├── common/
        │   │   ├── api/            # 统一响应
        │   │   ├── config/         # Spring 配置
        │   │   ├── exception/      # 全局异常
        │   │   └── model/          # 基础模型
        │   ├── security/           # deviceToken / adminToken 鉴权
        │   ├── modules/device/     # 注册、心跳、设备管理
        │   ├── modules/org/        # 机构管理
        │   ├── modules/region/     # 区域管理
        │   ├── modules/config/     # AI 配置、bootstrap
        │   ├── modules/prompt/     # Prompt 发布与 delta
        │   ├── modules/symptom/    # 症状模板管理与客户端 delta
        │   ├── modules/datapackage/# 映射数据包与 legacy template 包兼容
        │   ├── modules/audit/      # 审计事件与日志
        │   ├── modules/feedback/   # 用户反馈与调用链路聚合
        │   ├── modules/knowledge/  # PMPHAI/知识库代理
        │   ├── modules/adminui/    # 管理端静态页面入口控制
        │   └── modules/ai/         # chat / transcribe / realtime 代理
        └── main/
            ├── admin/              # 管理端 Vue 2 + Element UI 源码
            │   ├── package.json
            │   ├── vite.config.js
            │   └── src/
            └── resources/
                ├── application.yml   # 安全默认值 + 环境变量映射
                ├── mapper/
                ├── template-seeds/ # 症状模板内置基线 / 导入源
                ├── static/admin/   # 管理端构建产物落点
                └── sql/oracle/     # Oracle bootstrap/init scripts
```

### 3.1 运行配置安全模式

1. 仓库内提交的 `application.yml` 只保留安全默认值与环境变量映射，不再内置真实数据库地址、账号口令或 AES key。
2. 服务启动前必须注入 `FB_DB_URL`、`FB_DB_USERNAME`、`FB_DB_PASSWORD`、`FB_AES_KEY`。
3. PMPHAI 等上游服务地址在公开仓库中只保留示例地址；真实地址通过管理端配置或部署环境注入。
4. 本地联调如需私有覆盖配置，应使用未入库文件或部署环境变量，不得直接改回仓库默认值。

## 4. 后端分层

### 4.1 控制层

- `ClientController`：设备注册、心跳、bootstrap、delta、审计上报
- `AiProxyController`：聊天代理、语音转写、实时语音
- `PmphaiProxyController`：PMPHAI 搜索、详情、列表浏览、签名跳转
- `Admin*Controller`：区域、机构、设备、配置、Prompt、症状模板、数据包、日志

### 4.2 服务层

- 负责业务校验、配置分层查找、版本比较、上游代理转发
- 封装“机构级 > 区域级 > 全局级”的配置查找优先级
- `modules/config` 中的 AI 配置除主模型地址/密钥外，还负责托管独立审查 AI 与 PMPHAI 的服务端密钥；`bootstrap` 只下发非密钥视图
- `modules/symptom` 负责症状模板的逐条 CRUD、内置模板导入、JSON 模板文件导入、作用域合并与客户端 `templates/delta` 聚合，数据结构对齐 `floating-ball` 的 `SymptomManagement.vue` / disease editor
- `modules/datapackage` 继续负责映射数据包读取；`template` 类型数据包仅作为症状模板表未初始化时的兼容回退来源

### 4.3 数据访问层

- 使用 MyBatis-Plus `Mapper` 访问 Oracle 19c
- 首期不引入复杂读写分离、缓存和消息队列

### 4.4 管理端托管约定

- 管理端页面由 Spring Boot 统一托管，默认访问入口为 `/admin/`
- 管理端构建产物输出到 `server/src/main/resources/static/admin`
- 管理端接口继续使用 `/admin/api/*`，但页面与接口默认同源，不再依赖独立部署
- 如需单独前端调试，仍可在 `server/src/main/admin` 内运行 Vite dev server；此时后端 CORS 仅作为本地开发补充能力
- 远端 `/v1/*` 默认 CORS 需要兼容 `floating-ball` 的 Tauri dev / desktop WebView origin，且 `OPTIONS` 预检请求不能被设备鉴权拦截
- `floating-ball.cors.allowed-origins` 的本地配置只能做增量补充，不能覆盖掉桌面端默认 origin（`tauri://localhost`、`asset://localhost`、`https://tauri.localhost`、`http://tauri.localhost`、本地 localhost/127.0.0.1`），否则桌面端会在浏览器 Fetch 层直接报 `Load failed`

## 5. 核心业务链路

### 5.1 客户端启动链路

1. `floating-ball` 首次启动调用 `POST /v1/client/register`
2. 服务端返回 `deviceToken`
3. 客户端带 `Bearer deviceToken` 调用 `GET /v1/client/bootstrap`
4. 客户端按版本号调用 `prompts/templates/mappings delta`
5. 客户端周期性调用 `POST /v1/client/heartbeat`
6. 若客户端持久化的 `deviceToken` 因环境切换或服务端重建失效，客户端会清理本地注册缓存并重新执行 `register -> bootstrap`

删库重建补充约定：

1. Oracle `init.sql` 作为当前初始开发阶段的单一建库基线，直接包含 `c_ai_config` 的服务端托管字段，不依赖运行期回退
2. `init.sql` 会预置 `REGION001`、`ORG001`、`admin` 和作用域为 `ORG001` 的默认 AI 配置，保证 `register -> bootstrap -> audit` 链路可立即联调
3. 默认 AI 配置只保证启动链路与日志链路可用；真实上游 AI 地址、密钥、模型仍需在管理端修改

模板解析补充约定：

1. `GET /v1/client/templates/delta` 优先从 `c_ai_symptom_template` 合并当前作用域可见模板，合并优先级为机构级 > 区域级 > 全局级，同 `cd_symptom` 由高优先级记录覆盖
2. 管理端“症状模板”页面复用桌面端 disease editor 的逐条编辑思路，一条症状对应一条后台记录，而不是整包 JSON 覆盖
3. 管理端支持把桌面端已有 `templates.json`、`tcm-templates.json` 或后台导出的症状模板 JSON 重新导入 `c_ai_symptom_template`
4. 若当前作用域没有任何启用的症状模板记录，则兼容回退到已发布的 legacy `template` 数据包
5. 若既没有症状模板记录，也没有 legacy `template` 数据包，则回退到 `server/src/main/resources/template-seeds/` 内置症状模板基线

### 5.2 AI 代理链路

1. 客户端调用 `POST /v1/ai/chat`
2. 服务端通过 `deviceToken` 找到设备所属机构/区域
3. 按优先级查找 AI 配置
4. 转发到上游 OpenAI 兼容接口
5. 把非流式 JSON 或 SSE 流式结果回传给客户端

审查模型补充约束：

1. 当 `configProfile=reviewer` 时，服务端优先使用 AI 配置中的独立审查模型地址 / 密钥 / 模型；缺失时回退主模型配置
2. `bootstrap` 仅向桌面端暴露 `reviewer.enabled` 与 `reviewer.model` 等非敏感字段，不返回密钥
3. 区域化模式下，`/v1/ai/chat` 的实际生效模型以服务端当前解析到的配置为准；桌面端不应再依赖本地缓存的 `model` 回传覆盖服务端配置，确保后台修改后下一次请求立即生效

语音代理补充约束：

1. `floating-ball` 区域化模式下，`/v1/ai/speech/transcribe` 与 `/v1/ai/speech/realtime` 接收的是 base64 录音内容，而不是浏览器原生 `FormData`
2. 服务端必须先把 base64 录音解码为真实字节数组，再按上游 `/audio/transcriptions` 要求组装为 `multipart/form-data`
3. 对原始 PCM 录音，服务端先补 WAV 头后再上游转发，避免不同语音供应商对裸 PCM 兼容不一致
4. 服务端应保留录音元数据（`mimeType`、`format`、`fileName`、`scene`）用于排障和审计，但不在日志中落原始音频内容
5. 语音代理日志中的录音内容需要单独落为文件，默认写入 `floating-ball.audit.speech-file-dir` 指定目录；`c_ai_op_log` 只保存该录音文件路径，不把 base64 或二进制音频写入 `payload_json`

### 5.3 审计链路

1. 客户端本地缓存事件
2. 客户端对区域化操作日志不再依赖本地 SQLite，直接调用 `POST /v1/client/audit/events/batch`；启动时补传遗留队列，新事件入队后异步立即尝试一次，失败或离线时继续保留队列并按固定周期重试
3. 客户端对 `operation` 事件优先上报 `{ module, action, result, operationType, operationName, details }`；其中 `module/action/result` 是服务端日志列表列，`operationType/operationName/details` 继续保留在原始 payload
4. 客户端本地只保留轻量失败重试队列，不再把区域化操作日志落本地 SQLite；服务端仍按同一批量接口落库
5. 服务端兼容旧载荷：若未显式提供 `module/action/result`，则回退从 `operationType/operationName/success` 等字段推导
6. 服务端写入 `c_ai_op_log`
7. 管理端提供分页查询

代理日志补充约束：

1. 服务端对 `/v1/ai/chat`、`/v1/ai/speech/*` 等上游代理请求，除了成功/失败结果外，还应在 `payload_json` 中保留请求元数据与上游回文，便于排障
2. `speech_proxy` 日志的原始录音不得写入 `payload_json`；录音文件单独落盘，表中通过 `audio_file_path` 指向对应文件
3. API Key、Bearer Token 等凭据不得入库；除此之外，业务请求正文与回文可按原文保留
4. 管理端日志页应支持按 `ai_proxy`、`speech_proxy` 等代理日志类型筛选与查看详情

### 5.5 用户反馈链路

1. 桌面端区域化模式下，医生可提交“评分 + 说明 + 截图”反馈。
2. 反馈请求携带最近一次 AI 代理调用的 `traceId`、会话 ID、来源模块、请求/响应摘要等链路上下文。
3. 服务端保存反馈主体与截图数据，并按 `traceId` 优先、`sessionId + 时间窗口` 兜底聚合相关 `c_ai_op_log`。
4. 管理端“反馈管理”页面同时展示反馈内容、截图预览、上下文快照和调用链路时间线，便于排障。

### 5.4 PMPHAI 知识库代理链路

1. 区域化模式下，桌面端调用 `/v1/knowledge/pmphai/*`
2. 服务端按设备所属机构/区域解析当前可见 AI 配置
3. 服务端使用配置中的 PMPHAI `appKey/appSecret/baseUrl` 向上游申请 token 或生成签名 URL
4. 服务端把搜索结果、详情内容、列表浏览结果或页面 URL 返回给桌面端

约束：

1. PMPHAI 的 `appKey/appSecret` 只保留在服务端数据库或环境中，不能下发到桌面端
2. 区域化模式下，桌面端不再依赖 `src-tauri/src/http_server.rs` 的本地 `/api/pmphai/*` 代理

## 6. MVP 范围

### 6.1 第一阶段

1. `/v1/client/register`
2. `/v1/client/heartbeat`
3. `/v1/client/bootstrap`
4. `/v1/client/prompts/delta`
5. `/v1/client/templates/delta`
6. `/v1/client/mappings/delta`
7. `/v1/ai/chat`
8. `/v1/ai/speech/transcribe`
9. `/v1/client/audit/events/batch`
10. `/v1/knowledge/pmphai/search`
11. `/v1/knowledge/pmphai/clip`
12. `/v1/knowledge/pmphai/list`
13. `/v1/knowledge/pmphai/page-url`
10. 管理端最小 CRUD：
   - 完整 CRUD：区域、机构、设备、AI 配置、Prompt
   - 完整维护闭环：数据包
   - 查询增强：日志

### 6.2 第二阶段

1. 管理员登录与登录态校验
2. 用户、角色最小 CRUD
3. 概览统计与首页看板
4. 更细粒度权限和审计模型

### 6.3 当前实现假设

本轮按“最小闭环”推进第二阶段，只实现以下范围：

1. 轻量管理员鉴权：
   - `/admin/api/auth/login`
   - `/admin/api/auth/me`
   - `/admin/api/auth/logout`
   - `/admin/api/auth/password`
   - 管理端 `Bearer token` 鉴权
   - 管理端页面入口 `/admin/`
   - 支持“登录后自助修改密码”与“启动期受控重置管理员密码”
2. 用户管理：
   - 用户分页查询、新增、修改、停用
   - 用户与机构、角色关联
3. 角色管理：
   - 角色分页查询、新增、修改、停用
4. 概览统计：
   - 首页汇总区域、机构、设备、配置、Prompt、数据包、日志、用户、角色数量

约束：

1. 不引入 Redis、第三方 JWT 组件或复杂会话中心。
2. token 采用服务端轻量签发与校验，满足当前单体后台使用。
3. 统计首期直接基于现有业务表聚合，不新增统计中间表。

### 6.4 管理员密码维护约定

管理员密码采用双通道维护，分别解决日常改密与无法登录时的恢复场景：

1. 登录后自助修改：
   - 当前管理员登录后，可在管理端页头进入“修改密码”
   - 后端通过 `/admin/api/auth/password` 校验旧密码后更新 `c_ai_user.password_hash`
2. 启动期受控重置：
   - 服务启动时可读取 `floating-ball.admin.bootstrap-reset.*` 配置
   - 当 `enabled=true` 且提供了目标账号、新密码时，服务会在启动阶段把对应管理员密码重置为新值
   - 该机制只用于忘记密码或初始化恢复，不提供公开匿名 HTTP 重置接口

## 7. 数据模型基线

首期表按 PRD 和当前桌面端契约收敛，至少包括：

1. `c_ai_region`
2. `c_ai_org`
3. `c_ai_device`
4. `c_ai_config`
5. `c_ai_prompt`
6. `c_ai_data_package`
7. `c_ai_symptom_template`
8. `c_ai_op_log`
   - 代理日志主体仍保存在 `payload_json`
   - 语音代理的录音文件路径保存在 `audio_file_path`
9. `c_ai_feedback`
9. `c_ai_user`
10. `c_ai_role`
11. `c_ai_user_role`

扩展表如用户、角色、统计可在第二阶段补齐。

## 8. Oracle 初始化约定

Oracle 初始化拆成两步：

1. `server/src/main/resources/sql/oracle/bootstrap.sql`
   - 由 `SYSTEM` 或具备 DBA 权限的账号执行
   - 负责创建业务表空间；在使用独立 schema 时，再创建业务 schema/user 并授予最小必需权限
2. `server/src/main/resources/sql/oracle/init.sql`
   - 由当前应用连接账号登录后执行
   - 负责创建业务表、索引和种子数据
   - 不显式声明 `TABLESPACE`，由执行前切换好的 schema/默认表空间决定对象落点

说明：

1. 本项目不在业务初始化脚本中执行 `CREATE DATABASE`。Oracle 一般复用现有实例/服务，应用侧只负责 schema 层初始化。
2. 当前默认连接账号为 `SYSTEM`，因此 `bootstrap.sql` 会先建表空间、再跳过建用户步骤；`init.sql` 只保留建表语句，由执行前切换好的 schema/默认表空间决定对象落点。正式环境仍建议切回独立业务 schema。

## 9. 单体部署约定

本轮管理端改造采用“单体托管，不重写页面”的策略：

1. 保留现有 Vue 2 + Element UI 管理端实现，避免为“取消前后端分离”而重写成服务端模板。
2. 管理端源码迁入 `server` 模块，由同一套 Maven / Spring Boot 工程承载。
3. 发布结果为单个 Spring Boot 服务，前后端同端口、同域名、同进程。
4. 开发阶段允许保留 Vite 独立调试能力，但这不再是默认交付形态。
5. 管理端表单交互优先复用 `floating-ball` 现有设置页的分组式信息架构：按“基础信息 / 主模型 / 语音 / 知识库 / 审查模型 / 作用域与功能开关”分段展示，避免把所有配置字段平铺在单一网格中。
6. 服务端 AI / 语音上游出站请求允许通过 `floating-ball.ai.proxy.*` 配置显式走 HTTP 代理；在 macOS 开发环境下同时建议启用 Netty 的 native DNS 解析依赖，避免 Java 进程与终端 `curl` 的网络行为不一致。
7. AI 配置页应提供“服务端到上游 LLM”的单独测试入口，用于区分“floating-ball -> server”链路故障与“server -> LLM”链路故障。
