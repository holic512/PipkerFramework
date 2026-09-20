# Pipker Framework

Pipker Framework 是一个由 Spring Boot 后端与 Vue 前端组成的后台基础框架。当前第一阶段交付的是系统账户、数据库驱动 RBAC、Liquibase 数据库演进、Bearer 会话和数据库菜单驱动的动态路由；它刻意不包含商家、订单、商品、客户等业务实体。

## 本期能力与边界

- 9 张 `system_` 前缀的框架 Liquibase 系统表，以及由 changeset 管理的唯一初始管理员；用户表和对框架表的扩展保留在数据库专属的 `business` 迁移入口。
- 统一的 `SYSTEM` 登录域；用户—角色—权限 RBAC 由数据库裁决，`SUPER_ADMIN` 自动拥有全部启用权限。
- 角色—菜单关联控制数据库页面路由，`API` 权限通过 `HTTP 方法 + MVC 路径模板` 的数据库资源映射控制后端接口。
- Sa-Token Filter 统一执行登录校验，MVC 注解执行接口授权；用户授权快照使用本地 60 秒 TTL 缓存，启用角色到接口权限的映射在启动时载入内存并默认每 5 分钟刷新。
- `POST /api/auth/login`、当前 Token 登出、`GET /api/auth/me`、管理员登录日志查询与 `{ code, data, message }` 统一响应；已注册 API 的业务失败保持 HTTP 200。
- 登录、登出和 `/api/auth/me` 鉴权事件同步尽力写入独立的 `system_login_log` 审计表；日志长期保留且不保存密码、Token、Cookie、Authorization 请求头或请求体。
- 前端登录页、`sessionStorage` Bearer 令牌、授权恢复和仅由已授权角色菜单生成的 Vue 页面路由；包含仅供 `SUPER_ADMIN` 使用的角色路由配置页，不提供按钮级权限控制。
- 默认启用的本地文件持久化：后端服务保存后获得不含域名的 `/files/...` 相对访问路径，公开读取接口不提供 HTTP 上传。

本期不提供用户、角色、菜单或 API 权限 CRUD；仅提供由 `SUPER_ADMIN` 使用的角色路由菜单配置页面，也不创建任何业务表。后续业务身份应通过新增 `system_role.role_code` 和新的 Liquibase 增量 changeset 演进。

## 仓库结构

```text
PipkerFramework/
├── backend/   # Maven Reactor：Starter、业务 API、可执行 Server
└── frontend/  # Vue：会话、HTTP、动态路由、页面布局
```

- 后端模块边界、数据库 Profile、API 契约和安全说明见 [backend/README.md](backend/README.md)。
- 前端会话、动态组件键和本地运行方式见 [frontend/README.md](frontend/README.md)。
- 角色、页面、接口资源与缓存生效规则见 [docs/2.权限配置说明.md](docs/2.权限配置说明.md)。

## 快速开始（本地开发）

前置条件：JDK 21、Maven 3.9+、Node.js 与 npm。后端默认使用 `dev,sqlite`，首次启动会在 `backend/data/base/pipker.db` 创建 SQLite 数据库并由 Liquibase 初始化系统表；启动期还会预创建 `backend/data/file` 与 `backend/data/log`。文件保存功能使用 `backend/data/file`：

1. 启动后端；Liquibase 会在空库中创建全部 `system_` 表并写入种子数据。
2. 启动前端；开发服务器默认将 `/api` 和 `/files` 代理到 `http://localhost:8080`。

```bash
cd backend
mvn -pl pipker-server -am spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

选择其他数据库时，通过环境 Profile 组合切换。每次启动选择一个环境 Profile 和一个数据库 Profile：

```bash
# 开发环境 + PostgreSQL
cd backend
mvn -pl pipker-server -am spring-boot:run \
  -Dspring-boot.run.profiles=dev,pg
```

```bash
# 生产环境 + MySQL
cd backend
PIPKER_DATABASE_URL='jdbc:mysql://localhost:3306/pipker' \
PIPKER_DATABASE_USERNAME='pipker' \
PIPKER_DATABASE_PASSWORD='change-me' \
mvn -pl pipker-server -am spring-boot:run \
  -Dspring-boot.run.profiles=prod,mysql
```

可用数据库 Profile 为 `sqlite`、`mysql` 和 `pg`；生产环境必须显式指定数据库，不能只使用 `prod`。`PIPKER_DATA_ROOT` 可配置本地运行时数据根目录，默认为 `./data`；SQLite 固定使用 `${PIPKER_DATA_ROOT}/base/pipker.db`，本地文件固定使用 `${PIPKER_DATA_ROOT}/file`，`${PIPKER_DATA_ROOT}/log` 当前仅预留且不写入文件日志。MySQL、PostgreSQL 的外部连接信息继续使用 `PIPKER_DATABASE_URL`、`PIPKER_DATABASE_USERNAME` 和 `PIPKER_DATABASE_PASSWORD`。文件模块仍可用 `PIPKER_FILE_ENABLED` 和 `PIPKER_FILE_ACCESS_PATH` 覆盖开关、根相对读取前缀；文件服务返回 `/files/...` 一类的相对路径而不是完整 URL。不要把真实密码提交到配置文件或命令历史中。

空数据库的初始账户是 `admin / admin123`。该口令仅用于首次本地初始化，绝不能用于公开部署；上线前必须使用与 `SecurityCryptoService` 兼容的 `{bcrypt}` 哈希替换它。主配置中提交的 AES-GCM Base64 密钥同样只是本地开发默认值，生产部署必须替换。

## 验证

```bash
cd backend
mvn clean test
```

```bash
cd frontend
npm run build
```

后端集成测试统一使用隔离的 SQLite 空库，验证 Liquibase 初始化、种子幂等性、密码不以明文保存、认证生命周期审计、当前 Token 单独登出、页面菜单和本地授权缓存行为。MySQL 与 PostgreSQL 迁移资源用于实际部署；Docker/Testcontainers 不是本项目测试前置条件。
