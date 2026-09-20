# Pipker Framework

Pipker Framework 是一个由 Spring Boot 后端与 Vue 前端组成的后台基础框架。当前第一阶段交付的是系统账户、数据库驱动 RBAC、Liquibase 数据库演进、Bearer 会话和数据库菜单驱动的动态路由；它刻意不包含商家、订单、商品、客户等业务实体。

## 本期能力与边界

- 10 张 `system_` 前缀的框架 Liquibase 系统表，以及由 changeset 管理的唯一初始管理员；用户表和对框架表的扩展保留在数据库专属的 `business` 迁移入口。
- 统一的 `SYSTEM` 登录域；用户—角色—权限 RBAC 由数据库裁决，`SUPER_ADMIN` 自动拥有全部启用权限。
- 角色—菜单关联控制数据库页面路由，`API` 权限通过 `HTTP 方法 + MVC 路径模板` 的数据库资源映射控制后端接口。
- Sa-Token Filter 统一执行登录校验，MVC 注解执行接口授权；用户授权快照使用本地 60 秒 TTL 缓存，启用角色到接口权限的映射在启动时载入内存并默认每 5 分钟刷新。
- `POST /api/auth/login`、当前 Token 登出、`GET /api/auth/me`、管理员登录日志查询与 `{ code, data, message }` 统一响应；已注册 API 的业务失败保持 HTTP 200。
- 登录、登出和 `/api/auth/me` 鉴权事件同步尽力写入独立的 `system_login_log` 审计表；日志长期保留且不保存密码、Token、Cookie、Authorization 请求头或请求体。
- 所有 `/api/**` 请求由统一 Filter 与 Controller AOP 生成一条元数据访问记录，经有界队列异步批量写入 `system_api_access_log`；支持成功率、耗时趋势、接口聚合、慢请求与 TraceId 排查，并按 30 天默认保留期定时分批清理。
- 统一 Filter 同时为每次 HTTP 完成输出脱敏摘要；生产环境将完整控制台日志写入 `data/log/pipker-server.log`，并按日切片为不自动删除的 `.log` 归档。
- 前端登录页、`sessionStorage` Bearer 令牌、授权恢复和仅由已授权角色菜单生成的 Vue 页面路由；包含仅供 `SUPER_ADMIN` 使用的角色路由配置页，不提供按钮级权限控制。
- “系统管理 → 接口监控”提供时间范围、慢阈值、采集质量、组合趋势、接口分析和明细抽屉；概览默认排除监控接口自身，原始日志仍可检索这些请求。
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

可用数据库 Profile 为 `sqlite`、`mysql` 和 `pg`；生产环境必须显式指定数据库，不能只使用 `prod`。SQLite、本地文件与日志目录分别由 `PIPKER_SQLITE_ROOT`（默认 `./data/base`）、`PIPKER_FILE_ROOT`（默认 `./data/file`）和 `PIPKER_LOG_ROOT`（默认 `./data/log`）配置。`dev` 只预创建日志目录并输出控制台；`prod` 同时写入活动文件和 `archive/` 每日归档。MySQL、PostgreSQL 的外部连接信息继续使用 `PIPKER_DATABASE_URL`、`PIPKER_DATABASE_USERNAME` 和 `PIPKER_DATABASE_PASSWORD`。文件模块仍可用 `PIPKER_FILE_ENABLED` 和 `PIPKER_FILE_ACCESS_PATH` 覆盖开关、根相对读取前缀；不要把真实密码提交到配置文件或命令历史中。

## 日志职责边界

| 能力 | 数据去向 | 责任与保留策略 |
| --- | --- | --- |
| SLF4J 技术日志 | 全部环境控制台；`prod` 额外写文件 | 活动日志为 `pipker-server.log`，每日归档为未压缩 `.log`；应用不自动删除 |
| `@OperationLog` | 现有显式操作日志链路 | 由业务主动标注的操作语义，不作为接口性能统计数据源 |
| `system_login_log` | 数据库同步尽力写入 | 认证生命周期审计，长期保留，不参与接口日志清理 |
| `system_api_access_log` | 有界队列异步批量写入数据库 | `/api/**` 结果与性能元数据，默认保留 30 天；队列满时优先保证业务延迟并丢弃新观测记录 |

数据库接口访问记录只保存路由、用途、状态码、耗时、TraceId、客户端 IP 和经过清理截断的异常摘要，不保存查询参数或内容体。控制台/文件的 HTTP 摘要可额外包含脱敏截断后的 URL 查询参数，但不记录请求头、Cookie、Authorization、表单体、JSON 请求体或响应体。完整配置与管理 API 见 [backend/README.md](backend/README.md)。

`永久保留` 表示 Logback 不配置历史天数、总大小上限或启动清理；它不能防止人工删除、磁盘故障或临时容器文件系统丢失数据。生产环境必须为 `PIPKER_LOG_ROOT` 使用持久卷，监控磁盘容量，并根据运维要求备份。多实例部署应为每个实例分配独立日志目录。

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

后端集成测试统一使用隔离的 SQLite 空库，验证 Liquibase 初始化、种子幂等性、密码不以明文保存、认证生命周期审计、接口日志聚合与清理、当前 Token 单独登出、页面菜单和本地授权缓存行为；离线测试同时生成 MySQL、PostgreSQL 与 SQLite 迁移 SQL。Docker/Testcontainers 不是本项目测试前置条件。
