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
│   ├── pipker-spring-boot-starter-security/  # 密码哈希与字段加密
│   └── pipker-spring-boot-starter-file/      # 本地文件持久化与公开只读资源映射
├── pipker-business/
│   ├── pipker-business-common/               # API 响应、通用登录身份和公共异常契约
│   └── pipker-business-api/                  # system 功能包、HTTP 接口与数据访问
└── pipker-server/                             # Spring Boot 启动入口、数据库迁移、运行配置和集成测试
```

依赖方向固定如下：

- `pipker-server` 启动 `pipker-business-api`，并承载数据库、Redis、日志和文件存储运行配置。
- `pipker-business-api` 可以依赖 `pipker-business-common`、Sa-Token 与 Security Starter，不能依赖 Server。
- `pipker-business-common` 不依赖业务 API、Server 或技术 Starter。
- `pipker-spring-boot-starter-common` 只依赖 Java 标准库，提供 UUID、Base62 乱序标识符和按调用时采集的本机信息快照；不注册 Spring 自动配置，也不读取用户数据或认证信息。
- `pipker-spring-boot-starter-log` 单向依赖 Common Starter 生成 TraceId；Common Starter 不依赖任何其他 Starter。
- Sa-Token Starter 只提供会话与过滤器，不放置 User、Role、Mapper 或任何数据库授权逻辑。
- `pipker-spring-boot-starter-file` 提供本地文件服务、逻辑存储键和根相对访问路径；Servlet Web 应用中可按开关注册公开只读资源映射，但不提供 HTTP 上传、删除或目录枚举能力。

`pipker-business-api` 以业务功能而非分层目录组织：`system/auth` 负责认证和当前会话，`system/user` 负责账户，`system/authorization` 负责 RBAC 与菜单，`system/role` 负责角色生命周期与成员密码重置，`system/route` 只读查询已落库的路由定义，`system/permission` 提供 Java 权限枚举目录读取，`system/health` 负责存活检测；仅跨功能模型、Mapper 和 Web 异常映射位于 API 自身的 `common` 包。根 POM 管理 Spring Boot `4.1.1`、Java `21` 与内部模块版本。系统表 Mapper 使用 MyBatis-Plus `3.5.16`，由 `PipkerApplication` 的 `@MapperScan` 扫描；管理列表统一使用 MyBatis-Plus 分页拦截器，单页最大 100 条。

## 系统身份与 RBAC

三个概念必须分离：

| 概念 | 首期值/来源 | 用途 | 不是什么 |
| --- | --- | --- | --- |
| `system_user` | 数据库账户 | 凭证、状态与基本资料 | 角色定义或 Sa-Token LoginType |
| `system_role.role_code` | 如 `SUPER_ADMIN`、`ADMIN` | 授权分组 | 登录域或独立 StpLogic |
| `LoginType` | 固定 `SYSTEM` | 编码 Sa-Token 登录身份 | 用户角色或菜单权限 |

所有账户都使用默认 `StpUtil` 创建 `LoginIdentity(SYSTEM, userId)` 会话。Sa-Token Filter 对 `/api/**` 先执行登录校验，再由 MVC `PermissionAuthorizationInterceptor` 处理控制器上的 `@Permission(PermissionEnum.X)`；项目不使用 `@SaCheck*` 注解、独立 `StpLogic` 或 Controller 内手写权限检查。

`SUPER_ADMIN` 的特殊语义由授权模块集中维护：拥有全部**启用可见**导航菜单、全部启用页面路由和当前 `PermissionEnum` 定义的全部接口权限。普通账户合并其所有启用角色的页面路由及枚举权限；其中 `visible=false` 的页面会继续注册为受授权守卫保护的路由，但不会出现在导航中。授权采用两级本地缓存：启用角色到接口权限的不可变映射在启动时全量加载并默认每 5 分钟刷新，用户授权快照使用 Caffeine 进程内 60 秒 TTL 缓存。角色及接口权限管理事务提交后会重建角色权限映射，映射变化时清空用户快照；角色管理页也可手动刷新当前实例。详细配置见 [../docs/2.权限配置说明.md](../docs/2.权限配置说明.md)。

## 数据库与 Liquibase

全部 changelog 由可执行的 `pipker-server` 承载，并按数据库优先组织：

```text
pipker-server/src/main/resources/db/changelog/
├── sqlite/
│   ├── db.changelog-master.yaml
│   ├── system/                               # 001…010 框架系统迁移
│   └── business/                             # 用户 SQLite 表与框架扩展入口
├── mysql/
│   ├── db.changelog-master.yaml
│   ├── system/                               # 001…010 框架系统迁移
│   └── business/                             # 用户 MySQL 表与框架扩展入口
└── pg/
    ├── db.changelog-master.yaml
    ├── system/                               # 001…010 框架系统迁移
    └── business/                             # 用户 PostgreSQL 表与框架扩展入口
```

每个数据库总入口固定先加载 `system/db.changelog-master.yaml`，再加载 `business/db.changelog-master.yaml`。当前三个 `business` 入口均为空；后续用户新增业务表、自定义角色、权限、菜单，或对框架系统表的调整，必须作为新 changeset 追加到所选数据库的 `business` 目录，而不是修改既有 `system` changeset。

Liquibase 的 `DATABASECHANGELOG` 记录已执行 changeset；重复启动不会重复建表或写入种子数据。所有框架业务表均以 `system_` 开头：

| 表 | 责任 |
| --- | --- |
| `system_user` | 系统登录账户、`password_hash`、状态、最后登录时间及审计字段 |
| `system_role` | 角色编码、名称、状态和排序 |
| `system_permission` | 历史兼容的 `API` 权限定义，不是当前 MVC 注解授权的唯一来源 |
| `system_menu` | 路由与菜单的唯一持久化来源：目录或页面菜单、自关联 `parent_id`、路径、路由名、逻辑 `component_key` 和 `visible` 展示状态 |
| `system_user_role` | 用户与角色的独立雪花主键关联，并以外键对保持唯一 |
| `system_role_permission` | 角色与 `PermissionEnum` 编码的独立雪花主键关联，并以外键对保持唯一 |
| `system_role_menu` | 角色与可访问页面菜单的独立雪花主键关联，并以外键对保持唯一 |
| `system_api_resource` | 历史兼容的 `API` 路径映射，不是当前 MVC 注解授权的唯一来源 |

SQLite、MySQL 和 PostgreSQL 均使用独立的迁移树。SQLite 在建表阶段直接声明外键、联合主键、唯一约束和检查约束，以适配其不支持后置 `addForeignKeyConstraint`、`addUniqueConstraint` 的限制；MySQL 与 PostgreSQL 各自保留适用的约束调整 SQL。`009-system-database-rbac` 删除按钮和旧菜单关系；`010-system-role-route-menu` 恢复角色菜单关系。当前项目处于可重建阶段，不兼容改造前的 changelog 文件路径、changeset 历史或 `postgresql` Profile 别名。

初始数据包含 `SUPER_ADMIN`、`ADMIN`、系统概览、角色路由、角色管理、只读路由管理和只读权限点目录菜单，以及历史兼容权限表和当前角色权限关联。`SUPER_ADMIN` 不需要任何显式角色关联；`ADMIN` 通过追加式 changeset 获得权限点页面菜单关系，接口目录读取继续复用 `system:role:manage`。路由管理将 `system_menu` 作为路径、路由名、页面索引和菜单展示状态的唯一数据源；`DIRECTORY` 分类只组织导航层级，不需要真实页面文件或 `component_key`。不存在 `MERCHANT`、`USER` 或任何业务表。唯一初始管理员由 Liquibase 写入：`admin / admin123`，数据库只保存当前 `SecurityCryptoService` 可验证的 `{bcrypt}` 密码哈希。

> 安全警告：默认管理员口令只可用于首次本地初始化。公开部署前必须立即更换为受控的 `{bcrypt}` 哈希；不要把 `admin123` 用于共享或生产数据库。角色管理页面可为该角色成员设置新密码，但不会返回、记录或保存任何明文密码；非 `SUPER_ADMIN` 不能重置超级管理员账户。

### 选择部署数据库 Profile

Server 配置采用“主配置 → 环境入口 → 环境目录”的结构。主配置保留公共运行参数，`application-dev.yml` 和 `application-prod.yml` 分别导入开发、生产目录；数据库与本地文件默认值统一由 `config/base/` 负责，其中数据库 Profile 选择实际驱动：

```text
pipker-server/src/main/resources/
├── application.yml                         # 公共配置，默认 dev,sqlite
├── application-dev.yml                     # dev 入口
├── application-prod.yml                    # prod 入口
└── config/
    ├── base/
    │   ├── application.yml                 # 数据库与文件基础配置入口
    │   ├── database/
    │       ├── sqlite.yml
    │       ├── mysql.yml
    │       └── pg.yml
    │   └── file.yml                        # 本地文件存储开关、受控目录与访问前缀
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
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=dev,pg

# 生产环境 + MySQL
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=prod,mysql

# 生产环境 + PostgreSQL
mvn -pl pipker-server -am spring-boot:run -Dspring-boot.run.profiles=prod,pg
```

每次启动必须选择一个环境 Profile（`dev` 或 `prod`）和一个数据库 Profile（`sqlite`、`mysql` 或 `pg`）。只启用 `prod` 不会回退到开发环境的 SQLite 默认值，因缺少数据源配置而快速失败。

MySQL 和 PostgreSQL 的连接参数通过以下环境变量提供，避免开发和生产共用仓库内的连接信息：

```text
PIPKER_DATABASE_URL
PIPKER_DATABASE_USERNAME
PIPKER_DATABASE_PASSWORD
```

SQLite 固定使用 `${PIPKER_DATA_ROOT:./data}/base/pipker.db`，不支持通过 `PIPKER_DATABASE_URL` 或 `PIPKER_SQLITE_PATH` 单独覆盖其路径。MySQL、PostgreSQL 继续使用 `PIPKER_DATABASE_URL`、`PIPKER_DATABASE_USERNAME` 与 `PIPKER_DATABASE_PASSWORD` 提供外部连接信息；三个 Profile 分别使用 `com.mysql.cj.jdbc.Driver`、`org.postgresql.Driver` 和 `org.sqlite.JDBC`。

Redis 配置分别位于 `config/dev/redis.yml` 和 `config/prod/redis.yml`，使用 `spring.data.redis.host`、`port`、`database`、`username`、`password`。启用 Redis 会话存储时组合 `redis` Profile；默认使用内存会话存储，适合单实例开发。

### 本地文件持久化

`pipker-spring-boot-starter-file` 默认启用本地文件存储。Server 将全部本地运行数据收敛在 `PIPKER_DATA_ROOT`（默认 `./data`）中；它可配置为相对或绝对本地路径。应用启动时会创建固定布局：

```text
${PIPKER_DATA_ROOT:-./data}/
├── base/  # SQLite 数据库：pipker.db
├── file/  # FileStorageService 保存的文件
└── log/   # 仅预留，当前不写入文件日志
```

按照文档中的启动方式在 `backend/` 运行 Server 时，SQLite 路径为 `backend/data/base/pipker.db`，文件根目录为 `backend/data/file`。SQLite 和文件存储都不能通过独立路径配置离开该根目录；`data/log` 仅用于预留目录，不改变当前控制台日志行为。

默认配置位于 `pipker-server/src/main/resources/config/base/file.yml`：

```yaml
pipker:
  data:
    root: ${PIPKER_DATA_ROOT:./data}
  file:
    enabled: ${PIPKER_FILE_ENABLED:true}
    local:
      root: ${pipker.data.root}/file
    access-path: ${PIPKER_FILE_ACCESS_PATH:/files}
```

可通过 `PIPKER_DATA_ROOT` 配置唯一的本地运行时数据根目录；启动期会拒绝 SQLite 数据库或 `pipker.file.local.root` 被其他高优先级属性改到该目录之外。`PIPKER_FILE_ENABLED=false` 可完全关闭文件功能，关闭后不会注册 `FileStorageService`，`/files/**` 也不会映射到磁盘，访问结果保持标准 HTTP `404`；`data/file` 与 `data/log` 仍会作为固定本地目录被预创建。`PIPKER_FILE_ACCESS_PATH` 必须是安全的绝对 URL 路径前缀，例如 `/files` 或 `/public/files`，不能包含 `..`、通配符、查询参数或域名。

业务模块通过 `FileStorageService` 写入文件，而不是自行拼接本地路径：

```java
StoredFile stored = fileStorageService.store(inputStream, originalFilename);

String storageKey = stored.storageKey();   // 例如 2026/09/04/<32 位随机键>.png
String accessPath = stored.accessPath();   // 例如 /files/2026/09/04/<32 位随机键>.png
```

服务还提供 `store(byte[], String)`、`load(String)`、`exists(String)`、`delete(String)` 和 `accessPath(String)`；`FileStorageUtils` 可用于校验逻辑存储键、提取安全扩展名和拼接根相对访问路径。默认保存时使用 `yyyy/MM/dd/<32 位 Base62 随机键>[扩展名]`，不以原始文件名作为实际路径。所有调用方存储键都会拒绝绝对路径、`..`、反斜杠和其他不安全片段，写入先落到同目录临时文件后再移动到最终位置。

匿名 `GET` 或 `HEAD /files/**` 返回原始文件内容，不使用 API JSON 响应包装；不存在的文件保持 HTTP `404`。该模块没有 HTTP 上传、删除、目录列表或完整 URL 生成接口。`StoredFile.accessPath()` 永远不含协议、主机、端口、请求 Host、`X-Forwarded-*` 或本地 `file:` 路径，调用方、浏览器或网关应按当前部署的公开域名和前缀拼接它。

默认文件访问是公开的：任何能够猜到或获得文件路径的客户端都可以读取对应内容。因此不要把需要逐文件鉴权的敏感资料放入该目录；生产反向代理必须将 `/files/**` 与 `/api` 一同转发给后端。`backend/data/` 已被 Git 忽略，避免本地 SQLite 数据库、保存文件和预留日志目录进入版本控制。旧布局的 `data/pipker.db` 与 `data/files` 不会自动迁移；当前框架开发阶段可在切换前停止应用并清空旧本地 `data/` 内容，再由下次启动重建固定目录。

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
| `GET /api/auth/me` | 已登录且具备 `system:authorization:view` | 当前用户、角色编码、权限编码、可见菜单树和全部已授权页面路由 |
| `GET /api/admin/authorization` | 已登录且具备 `system:authorization:view` | 当前授权投影 |
| `/api/admin/roles` | 已登录且具备 `system:role:manage` | 角色筛选分页、增删改、批量状态/删除、详情、页面与接口权限、成员分页与成员密码重置 |
| `POST /api/admin/roles/permission-cache/refresh` | 已登录且具备 `system:role:manage` | 全量刷新当前应用实例的角色接口权限一级缓存并返回刷新统计 |
| `GET /api/admin/permissions` | 已登录且具备 `system:role:manage` | 只读返回当前 `PermissionEnum` 的全部有效权限点，按枚举声明顺序排列 |
| `GET /api/admin/routes`、`GET /api/admin/routes/{routeId}` | 已登录且具备 `system:route:view` | 只读筛选、分页和查看已落库路由、页面索引、菜单展示状态与目录分类 |

Sa-Token 只显式放行 `GET /api/ping` 和 `POST /api/auth/login`。其他 `/api/**` 请求先完成登录校验，再由 MVC `PermissionAuthorizationInterceptor` 读取接口或控制器上的 `@Permission(PermissionEnum.X)`，并在当前授权快照中匹配冒号格式编码。`system_permission` 与 `system_api_resource` 继续保留为历史兼容结构，不再作为当前注解授权的唯一来源；前端唯一的页面路由来源是 `/api/auth/me`。

权限点目录页面位于“系统管理 → 权限点”，通过数据库菜单的 `componentKey = system/permission/index` 动态装载。页面只提供前端筛选、树形展开与查看，不提供权限点 CRUD；权限编码详情见 [../docs/2.权限配置说明.md](../docs/2.权限配置说明.md)。

## 本地构建与测试

前置条件：JDK `21`、Maven `3.9+`。在本目录执行：

```bash
mvn clean test
```

Server 集成测试统一使用隔离的临时 SQLite 空库，验证 SQLite 专用 changelog、8 张目标 `system_` 表、约束、种子数据、数据库 API 过滤器、角色并集、页面菜单、本地缓存、角色 CRUD、分页、批量操作、成员密码重置及重复执行幂等性。MySQL 与 PostgreSQL 迁移资源在部署环境使用；本项目不将 Docker 或 Testcontainers 作为测试前置条件。
