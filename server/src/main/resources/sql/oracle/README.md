# Oracle 初始化说明

Oracle 初始化分两层执行：

1. `bootstrap.sql`
   - 使用 `SYSTEM` 或具备 DBA 权限的账号执行
   - 默认会准备应用 schema：`RBMH_AI / RBMH_AI`
   - 作用：创建业务表空间；在使用独立 schema 时创建业务 schema/user，并授予建表所需权限
2. `init.sql`
   - 必须使用当前应用连接账号登录后执行
   - 当前仓库已切换到安全模式，`application.yml` 不再内置 `FB_DB_URL / FB_DB_USERNAME / FB_DB_PASSWORD` 默认值
   - 作用：创建业务表、索引和默认种子数据
   - 说明：不显式声明 `TABLESPACE`，由当前登录用户/默认表空间决定对象落点；脚本只保留标准 Oracle DDL/DML，避免 `SET/DEFINE` 之类 SQL*Plus 指令在通用客户端里报 `ORA-00922`

## 为什么没有 `CREATE DATABASE`

Oracle 通常不会像 MySQL 一样在应用脚本里直接执行 `CREATE DATABASE`。
在当前项目里：

- 数据库实例 / SID / Service 由 DBA 预先准备
- 应用侧只负责 schema(user) 级初始化

## 推荐执行顺序

### 1. DBA 账号执行

```sql
@bootstrap.sql
```

当前默认目标用户：

- schema/user: `RBMH_AI`
- password: `RBMH_AI`
- tablespace: `FLOATING_BALL_TS`

说明：以上仅是 `bootstrap.sql` 的初始化模板，不代表仓库中的运行时默认连接信息。

如果后续要改回独立业务 schema，再修改 `bootstrap.sql` 中的：

- `v_tablespace_name`
- `v_tablespace_file`
- `v_target_user`
- `v_target_password`

默认模式下，`bootstrap.sql` 会：

1. 创建表空间
2. 创建或复用 `RBMH_AI`
3. 为 `RBMH_AI` 授权并设置默认表空间
4. 由执行人员切换到与 `FB_DB_USERNAME` 一致的 schema 后，再执行 `init.sql`

### 2. 使用当前应用账号执行

```sql
@init.sql
```

`init.sql` 当前是“删库重建”的权威基线，已包含：

1. `c_ai_config` 的语音独立密钥、PMPHAI / Reviewer 服务端托管字段
2. 默认区域 `REGION001`
3. 默认机构 `ORG001`
4. 默认管理员 `admin`
5. 默认 AI 配置 `CFG001`
6. 脚本末尾显式 `COMMIT`

说明：

1. 默认 AI 配置仅用于打通 `register -> bootstrap -> audit` 的启动联调链路
2. 真正的上游 AI 地址、密钥、模型请在删库重建后再通过管理端修改
3. 初始开发阶段优先采用“删库重建 + 重跑 `init.sql`”，不要优先走增量补丁思路
4. 执行 `init.sql` 前请确认当前登录 schema 就是 `RBMH_AI`；脚本本身不再依赖 SQL*Plus 变量做前置校验

## 存量库升级

如果库里已经存在旧版 `c_ai_config`，且当前环境明确不能删库重建，再使用当前应用账号执行：

```sql
@upgrade_20260421_ai_config_server_managed.sql
@upgrade_20260428_ai_config_audio_key.sql
```

这些脚本会为旧表补齐以下服务端托管字段：

1. `pmphai_enabled`
2. `pmphai_base_url`
3. `pmphai_app_key_encrypted`
4. `pmphai_app_secret_encrypted`
5. `reviewer_enabled`
6. `reviewer_base_url`
7. `reviewer_api_key_encrypted`
8. `reviewer_model`
9. `audio_api_key_encrypted`

## 症状模板表升级

如果库里已经存在旧版业务表，但还没有症状模板表，请继续使用当前应用账号执行：

```sql
@upgrade_20260421_symptom_template.sql
```

说明：

1. 该脚本会补建 `c_ai_symptom_template` 及相关索引
2. `c_ai_symptom_template` 采用“一条症状一条记录”的结构，承接桌面端 disease editor 的后台化改造
3. 模板的复杂字段（`config`、`applicablePopulation`、`tcmMetadata`、系统分类/部位数组）会以 JSON 片段方式存入表字段
4. 表结构创建完成后，可在管理端“症状模板”页面使用“导入内置模板”把 `template-seeds` 的西医/中医基线导入指定作用域

## 语音代理日志录音路径升级

如果库里已经存在旧版 `c_ai_op_log`，但还没有录音文件路径字段，请继续使用当前应用账号执行：

```sql
@upgrade_20260422_op_log_audio_file_path.sql
```

说明：

1. 该脚本会为 `c_ai_op_log` 补齐 `audio_file_path`
2. 升级完成后，语音代理日志不再把原始 base64 音频写入 `payload_json`
3. 录音内容会单独落为文件，数据库只保存对应文件路径

## 结构化操作日志查询列升级

如果库里已经存在旧版 `c_ai_op_log`，但还没有结构化查询列，请继续使用当前应用账号执行：

```sql
@upgrade_20260506_op_log_structured_query.sql
```

说明：

1. 该脚本会为 `c_ai_op_log` 补齐 `op_action`、`op_title`、`source_module`、`scene_code`、`trace_id`
2. 升级完成后，管理端操作日志页可直接按动作编码、标题、来源模块、场景、traceId 查询
3. 旧数据不会自动回填这些新列；如需对历史日志做精细查询，可执行额外数据修复脚本或接受仅对新日志生效

## 运维用户日志语音复盘升级

如果库里已经存在旧版 `c_ai_user_consultation_log`，但还没有语音问诊录音和 ASR 文本字段，请继续使用当前应用账号执行：

```sql
@upgrade_20260428_user_consultation_log_audio.sql
```

说明：

1. 该脚本会为 `c_ai_user_consultation_log` 补齐 `speech_text`、`audio_file_path`、`audio_file_name`、`audio_mime_type`、`audio_size`
2. 升级完成后，桌面端语音问诊会把录音和识别文字追加到同一条用户日志
3. 录音内容会单独落为文件，数据库只保存对应文件路径和元数据，后台详情通过鉴权接口播放

## 如果暂时继续使用 `SYSTEM`

如果你刻意要直接用 `SYSTEM` 账号连应用：

1. 同步把运行环境变量中的 `FB_DB_USERNAME / FB_DB_PASSWORD` 改成 `SYSTEM`
2. 把 `bootstrap.sql` 里的 `v_target_user / v_target_password` 改成 `SYSTEM`
3. `bootstrap.sql` 先创建表空间，再自动跳过建用户
4. 直接用 `SYSTEM` 执行 `init.sql`

但这只适合临时开发联调，不建议作为正式部署方案。
