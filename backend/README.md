# Pipker Framework Backend

Pipker Framework 的 Maven 后端工程。当前以 Starter 技术基础设施、业务 API 和 `pipker-server` 组装层划分职责；第一阶段只承载系统账户和 RBAC，不承载任何商家、订单、商品或客户领域模型。

## 模块边界

```text
backend/
├── pipker-starters/
│   ├── pipker-spring-boot-starter-common/    # 纯 JDK UUID、乱序标识符与本机信息工具
│   ├── pipker-spring-boot-starter-redis/     # Redis 基础设施
│   ├── pipker-spring-boot-starter-log/       # TraceId、日志与脱敏
│   ├── pipker-spring-boot-starter-satoken/   # Bearer 会话与路由保护
│   └── pipker-spring-boot-starter-security/  # 密码哈希与字段加密
├── pipker-business/
│   ├── pipker-business-common/               # API 响应、通用登录身份和公共异常契约
│   └── pipker-business-api/                  # system 功能包、HTTP 接口、数据访问与 Liquibase changelog
└── pipker-server/                             # Spring Boot 启动入口、运行配置和集成测试
```

依赖方向固定如下：

- `pipker-server` 启动 `pipker-business-api`，并承载数据库、Redis 和日志运行配置。
- `pipker-business-api` 可以依赖 `pipker-business-common`、Sa-Token 与 Security Starter，不能依赖 Server。
- `pipker-business-common` 不依赖业务 API、Server 或技术 Starter。
- `pipker-spring-boot-starter-common` 只依赖 Java 标准库，提供 UUID、Base62 乱序标识符和按调用时采集的本机信息快照；不注册 Spring 自动配置，也不读取用户数据或认证信息。
- `pipker-spring-boot-starter-log` 单向依赖 Common Starter 生成 TraceId；Common Starter 不依赖任何其他 Starter。
- Sa-Token Starter 只提供会话与过滤器，不放置 User、Role、Mapper 或任何数据库授权逻辑。

`pipker-business-api` 以业务功能而非分层目录组织：`system/auth` 负责认证和当前会话，`system/user` 负责账户，`system/authorization` 负责 RBAC 与菜单，`system/health` 负责存活检测；仅跨功能模型和 Web 异常映射位于 API 自身的 `common` 包。根 POM 管理 Spring Boot `4.1.1`、Java `21` 与内部模块版本。系统查询 Mapper 使用 MyBatis Spring Boot Starter `4.0.1`，由 `PipkerApplication` 的 `@MapperScan` 扫描。

## 系统身份与 RBAC

三个概念必须分离：

| 概念 | 首期值/来源 | 用途 | 不是什么 |
| --- | --- | --- | --- |
| `system_user` | 数据库账户 | 凭证、状态与基本资料 | 角色定义或 Sa-Token LoginType |
| `system_role.role_code` | 如 `SUPER_ADMIN`、`ADMIN` | 授权分组 | 登录域或独立 StpLogic |
| `LoginType` | 固定 `SYSTEM` | 编码 Sa-Token 登录身份 | 用户角色或菜单权限 |

所有账户都使用默认 `StpUtil` 创建 `LoginIdentity(SYSTEM, userId)` 会话。业务 API `system/auth` 中的 `SystemStpInterface` 每次权限或角色检查都从数据库读取最新关联；没有按角色划分的 `StpLogic`，也没有首期缓存。

`SUPER_ADMIN` 的特殊语义只集中在 `SystemAuthorizationService`：拥有全部启用权限和可见菜单。普通账户通过用户角色关联合并权限和菜单。数据库直接修改权限、菜单或关联关系后，下一次请求即可生效。

## 数据库与 Liquibase

主 changelog 位于：

```text
pipker-business/pipker-business-api/src/main/resources/db/changelog/
├── db.changelog-master.yaml
└── system/
    ├── 001-system-user.yaml
    ├── 002-system-role.yaml
    ├── 003-system-user-role.yaml
    ├── 004-system-permission.yaml
    ├── 005-system-role-permission.yaml
    ├── 006-system-menu.yaml
    ├── 007-system-role-menu.yaml
    └── 008-system-init-data.yaml
```

Liquibase 的 `DATABASECHANGELOG` 记录已执行 changeset；重复启动不会重复建表或写入种子数据。所有框架业务表均以 `system_` 开头：

| 表 | 责任 |
| --- | --- |
| `system_user` | 系统登录账户、`password_hash`、状态、最后登录时间及审计字段 |
| `system_role` | 角色编码、名称、状态和排序 |
| `system_permission` | `API` / `BUTTON` 类型权限编码 |
| `system_menu` | 目录、菜单或按钮；自关联 `parent_id`、路由与逻辑 `component_key` |
| `system_user_role` | 用户与角色的联合主键关联 |
| `system_role_permission` | 角色与权限的联合主键关联 |
| `system_role_menu` | 角色与菜单的联合主键关联 |

MySQL、PostgreSQL 和 H2 使用上面的通用 changelog。SQLite 使用单独的 `db/changelog/sqlite/` changelog：它在建表阶段直接声明外键、联合主键、唯一约束和检查约束，以适配 SQLite 不支持后置 `addForeignKeyConstraint`、`addUniqueConstraint` 的限制。两套 changelog 保持相同的表结构和初始数据，已有 MySQL/PostgreSQL 数据库的 Liquibase 历史不受影响。

初始数据只有 `SUPER_ADMIN`、`ADMIN`、最小框架权限、系统目录和 `system/overview/index` 菜单。不存在 `MERCHANT`、`USER` 或任何业务表。唯一初始管理员由 Liquibase 写入：`admin / admin123`，数据库只保存当前 `SecurityCryptoService` 可验证的 `{bcrypt}` 密码哈希。

> 安全警告：默认管理员口令只可用于首次本地初始化。公开部署前必须立即更换为受控的 `{bcrypt}` 哈希；不要把 `admin123` 用于共享或生产数据库。首期没有密码重置、随机 Bootstrap 密码或 `system_bootstrap_state` 表。

### 选择部署数据库 Profile

Server 配置采用“主配置 → 环境入口 → 环境目录”的结构。主配置保留公共运行参数，`application-dev.yml` 和 `application-prod.yml` 分别导入开发、生产目录；数据库统一由 `config/base/` 负责，并通过数据库 Profile 选择实际驱动：

```text
pipker-server/src/main/resources/
├── application.yml                         # 公共配置，默认 dev,sqlite
├── application-dev.yml                     # dev 入口
├── application-prod.yml                    # prod 入口
└── config/
    ├── base/
    │   ├── application.yml                 # 数据库统一入口
    │   └── database/
    │       ├── sqlite.yml
    │       ├── mysql.yml
    │       └── postgresql.yml
    ├── dev/
    │   ├── application.yml                 # 开发配置
    │   └── redis.yml                        # redis Profile 配置
    └── prod/
        ├── application.yml                 # 生产配置
        └── redis.yml                        # redis Profile 配置
```

无显式 Profile 时默认使用 `dev,sqlite`：

```bash
mvn -pl pipker-server -am spring-boot:run
```

切换数据库时，在环境 Profile 后追加一个数据库 Profile：

```bash
# 开发环境 + SQLite
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=dev,sqlite

# 开发环境 + PostgreSQL
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=dev,postgresql

# 生产环境 + MySQL
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=prod,mysql

# 生产环境 + PostgreSQL
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=prod,postgresql
```

每次启动必须选择一个环境 Profile（`dev` 或 `prod`）和一个数据库 Profile（`sqlite`、`mysql` 或 `postgresql`）。只启用 `prod` 不会回退到开发环境的 SQLite 默认值，因缺少数据源配置而快速失败。

MySQL 和 PostgreSQL 的连接参数通过以下环境变量提供，避免开发和生产共用仓库内的连接信息：

```text
PIPKER_DATABASE_URL
PIPKER_DATABASE_USERNAME
PIPKER_DATABASE_PASSWORD
PIPKER_SQLITE_PATH
```

SQLite 默认使用 `./data/pipker.db`，可以通过 `PIPKER_SQLITE_PATH` 指定其他文件路径。MySQL、PostgreSQL 与 SQLite 分别使用 `com.mysql.cj.jdbc.Driver`、`org.postgresql.Driver` 和 `org.sqlite.JDBC`。

Redis 配置分别位于 `config/dev/redis.yml` 和 `config/prod/redis.yml`，使用 `spring.data.redis.host`、`port`、`database`、`username`、`password`。启用 Redis 会话存储时组合 `redis` Profile；默认使用内存会话存储，适合单实例开发。

主 [application.yml](pipker-server/src/main/resources/application.yml) 包含一个直接提交的本地 AES-GCM Base64 密钥，使 `SecurityCryptoService` 可以启动。它不是生产密钥，生产部署必须替换为受控的 32 字节 Base64 AES 密钥。数据库连接参数使用环境变量占位符，不在仓库内保存真实凭据。

## 认证、授权与 API 契约

令牌只从请求头读取：

```http
Authorization: Bearer <accessToken>
```

所有**已注册** API 都使用 HTTP `200` 和统一 JSON 包装：

```json
{ "code": 200, "data": {}, "message": "OK" }
```

`CommonApiCode` 集中维护框架默认的数值业务状态码和消息：成功为 `200`；参数校验失败为 `400`；无效凭据和未认证为 `401`；账户禁用和无权限为 `403`；服务器内部错误为 `500`。前端必须按响应体的 `code` 处理业务失败，不应把 HTTP 状态当成已注册接口的业务分支。

公共 `ApiCode` 是包含 `getCode()` 和 `getMessage()` 的接口。业务模块可让自己的枚举实现该接口，并直接传给 `ApiResponse.success(code, data)` 或 `ApiResponse.failure(code)`；后者自动使用枚举维护的默认消息。只有参数字段等需要额外上下文的场景才使用 `ApiResponse.failure(code, message)` 覆盖默认消息。

| 接口 | 访问规则 | 返回 |
| --- | --- | --- |
| `GET /api/ping` | 匿名 | 包装后的健康文本 |
| `POST /api/auth/login` | 匿名 | `accessToken`、`tokenType: Bearer` 和不含密码/电话/邮箱的用户资料 |
| `GET /api/auth/me` | 已登录 | 当前用户、角色编码、权限编码和数据库菜单树 |
| `GET /api/admin/authorization` | 已登录且具备 `system:authorization:read` | 当前授权投影；`/api/admin/**` 权限保护基线 |
| `GET /api/_dev/routes` | 仅开发开关启用时匿名 | 当前菜单的 `path`、`name`、`componentKey`、`permission` |

Sa-Token 显式放行 `GET /api/ping`、`POST /api/auth/login` 和 `GET /api/_dev/routes`。Route Manifest 由 `pipker.dev.route-manifest.enabled=true` 通过条件化 Controller 注册；关闭时 Controller 根本不存在，所以该 URL 保持标准 HTTP `404`，而不是统一业务错误。开启后它实时读取数据库，且绝不返回账户、令牌、密码、Redis 或加密数据。

## 本地构建与测试

前置条件：JDK `21`、Maven `3.9+`。在本目录执行：

```bash
mvn clean test
```

Server 集成测试使用 H2 和 SQLite 空库：H2 回归验证通用 changelog、认证/RBAC、动态菜单和 Route Manifest；SQLite 验证 SQLite 专用 changelog、七张 `system_` 表、约束、种子数据以及重复执行幂等性。H2 为兼容 `system_user` 标识符仅在测试连接中设置 `NON_KEYWORDS=SYSTEM_USER`；不影响 MySQL、PostgreSQL 与 SQLite 的实际表名。
