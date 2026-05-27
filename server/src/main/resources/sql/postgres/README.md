# PostgreSQL 初始化说明

PostgreSQL 初始化只负责应用库内对象：

1. 先由 DBA 或本地开发者创建数据库和应用账号
2. 使用与 `FB_DB_USERNAME` 一致的账号连接目标数据库
3. 执行 `init.sql`

示例：

```sql
CREATE DATABASE floating_ball;
CREATE USER floating_ball WITH PASSWORD 'floating_ball';
GRANT ALL PRIVILEGES ON DATABASE floating_ball TO floating_ball;
```

连接到 `floating_ball` 后执行：

```sql
\i init.sql
```

`init.sql` 是删库重建基线，包含当前 Java 实体使用的业务表、索引和默认种子数据：

1. 默认区域 `REGION001`
2. 默认机构 `ORG001`
3. 默认 AI 配置 `CFG001`
4. 默认角色 `ROLE001`
5. 默认管理员 `admin`
6. 默认用户角色关系 `USERROLE001`

运行 PostgreSQL profile 时建议配置：

```bash
SPRING_PROFILES_ACTIVE=postgres
FB_DB_TYPE=postgres
FB_DB_DRIVER_CLASS_NAME=org.postgresql.Driver
FB_DB_URL=jdbc:postgresql://127.0.0.1:5432/floating_ball
FB_DB_USERNAME=floating_ball
FB_DB_PASSWORD=floating_ball
```
