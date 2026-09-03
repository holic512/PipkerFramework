# Pipker Framework

Pipker Framework 是一个由 Spring Boot 后端与 Vue 前端组成的后台基础框架。当前第一阶段交付的是系统账户、实时 RBAC、Liquibase 数据库演进、Bearer 会话和数据库菜单驱动的动态路由；它刻意不包含商家、订单、商品、客户等业务实体。

## 本期能力与边界

- `system_` 前缀的 Liquibase 系统表，以及由 changeset 管理的唯一初始管理员。
- 统一的 `SYSTEM` 登录域，实时查询的用户—角色—权限—菜单 RBAC 关系和 Sa-Token 权限校验。
- `POST /api/auth/login`、`GET /api/auth/me`、`GET /api/admin/authorization` 与受配置控制的开发 Route Manifest。
- `{ code, data, message }` 统一响应，已注册 API 的业务失败保持 HTTP 200；关闭 Route Manifest 时，该 Controller 不注册并返回 HTTP 404。
- 前端登录页、`sessionStorage` Bearer 令牌、授权恢复和由数据库菜单唯一驱动的 Vue 路由。

本期明确**不提供**用户、角色、权限或菜单 CRUD API/页面，也不创建任何业务表。后续业务身份应通过新增 `system_role.role_code` 和新的 Liquibase 增量 changeset 演进。

## 仓库结构

```text
PipkerFramework/
├── backend/   # Maven Reactor：Starter、业务 API、可执行 Server
└── frontend/  # Vue：会话、HTTP、动态路由、页面布局
```

- 后端模块边界、数据库 Profile、API 契约和安全说明见 [backend/README.md](backend/README.md)。
- 前端会话、动态组件键和本地运行方式见 [frontend/README.md](frontend/README.md)。

## 快速开始（本地开发）

前置条件：JDK 21、Maven 3.9+、Node.js 与 npm。后端默认使用 `dev,sqlite`，首次启动会在 `backend/data/pipker.db` 创建 SQLite 数据库并由 Liquibase 初始化系统表：

1. 启动后端；Liquibase 会在空库中创建全部 `system_` 表并写入种子数据。
2. 启动前端；开发服务器默认将 `/api` 代理到 `http://localhost:8080`。

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
  -Dspring-boot.run.profiles=dev,postgresql
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

可用数据库 Profile 为 `sqlite`、`mysql` 和 `postgresql`；生产环境必须显式指定数据库，不能只使用 `prod`。数据库连接信息使用 `PIPKER_DATABASE_URL`、`PIPKER_DATABASE_USERNAME` 和 `PIPKER_DATABASE_PASSWORD` 提供，SQLite 文件路径可通过 `PIPKER_SQLITE_PATH` 覆盖。不要把真实密码提交到配置文件或命令历史中。

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

后端集成测试使用 H2 验证 Liquibase 空库初始化、种子幂等性、密码不以明文保存、认证/RBAC、动态菜单和 Route Manifest。MySQL 或 PostgreSQL 是实际部署数据库；Docker/Testcontainers 不是本项目测试前置条件。
