# AGENTS.md

`floating-ball-server` 项目的协作规则。

本项目是 `floating-ball` 的配套后台，当前采用：

1. `server/`：Spring Boot 2.7 + Java 8 + Oracle 19c + MyBatis-Plus
2. `server/src/main/admin/`：Vue 2 + Element UI 管理端源码，由 `server/` 统一托管
3. `API.md`：远端 `/v1/*` 契约文档

## 必读顺序

1. 先读 [ARCHITECTURE.md](./ARCHITECTURE.md)
2. 再读 [API.md](./API.md)
3. 涉及需求来源时，读 [../rbmh-ai-platform/PRD.md](../rbmh-ai-platform/PRD.md)
4. 涉及桌面端真实调用时，读：
   - [../floating-ball/src/services/regionalClient.ts](../floating-ball/src/services/regionalClient.ts)
   - [../floating-ball/src/services/llm.ts](../floating-ball/src/services/llm.ts)
   - [../floating-ball/src/services/templateService.ts](../floating-ball/src/services/templateService.ts)
   - [../floating-ball/src/services/medicalData.ts](../floating-ball/src/services/medicalData.ts)
   - [../floating-ball/src/services/promptOverride.ts](../floating-ball/src/services/promptOverride.ts)
   - [../floating-ball/src/services/auditUploader.ts](../floating-ball/src/services/auditUploader.ts)

## 强制流程

1. 文档先行：架构、接口、数据模型、目录职责变化，先改文档再改代码。
2. 契约优先：`/v1/*` 契约变更时，必须同时更新 `API.md` 与 `floating-ball` 调用方。
3. 交付顺序：`ARCHITECTURE/API/AGENTS -> 代码 -> 构建/测试验证`
4. 管理端目录、构建命令、托管入口变化时，必须同步更新 `ARCHITECTURE.md` 与本文件。

## 硬约束

1. 不允许把本地 `/api/consultation/*` 逻辑直接搬进本项目；本项目只负责远端 `/v1/*` 与管理端。
2. 不允许只按旧 PRD 实现远端接口而忽略 `floating-ball` 当前真实字段。
3. 设备鉴权必须使用 `Authorization: Bearer {deviceToken}`，不得私自改成其他客户端认证方式。
4. `bootstrap`、`templates/mappings delta`、审计事件结构必须优先兼容 `floating-ball` 现有实现。
5. 未经明确要求，不引入 Redis、RocketMQ、微服务拆分等额外依赖。

## 当前阶段目标

1. 第一优先级：跑通 `floating-ball` 的远端客户端接口。
2. 第二优先级：补最小管理端 CRUD，支撑配置、Prompt、数据包和日志管理。
3. 第三优先级：逐步补用户、角色、统计等平台能力。

## 最小质量门禁

1. `server` 至少执行 `mvn -f server/pom.xml test` 或 `mvn -f server/pom.xml package`
2. 管理端至少执行 `npm --prefix server/src/main/admin run build`
3. 若无法完成构建，必须说明阻塞原因，并补充静态审查结论

## 关键联调清单

1. `POST /v1/client/register` 返回的 `deviceToken` 能被 `floating-ball` 缓存并复用
2. `GET /v1/client/bootstrap` 返回结构兼容 `floating-ball/src/services/regionalClient.ts`
3. `GET /v1/client/prompts/delta` 返回结构兼容 `promptOverride.ts`
4. `GET /v1/client/templates/delta` 返回结构兼容 `templateService.ts`
5. `GET /v1/client/mappings/delta` 返回结构兼容 `medicalData.ts`
6. `POST /v1/client/audit/events/batch` 能接收 `auditUploader.ts` 当前上传结构
7. `POST /v1/ai/chat` 非流式和流式响应都能被 `llm.ts` 正常消费
