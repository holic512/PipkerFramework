# Pipker Framework Frontend

Pipker Framework 的 Vue 3 管理端。前端不维护静态业务导航：SYSTEM 用户登录后，由后端 `GET /api/auth/me` 返回的数据库菜单树是动态页面路由的唯一来源。

## 技术基线

| 类别 | 已锁定版本 | 用途 |
| --- | --- | --- |
| Vue | `3.5.42` | 视图层与组合式 API |
| TypeScript | `6.0.3` | 应用与接口的静态类型检查 |
| Vite | `8.2.2` | 开发服务器与生产构建 |
| Vue Router | `5.3.0` | 登录、应用壳与动态页面路由 |
| Pinia | `4.0.3` | 会话、授权与布局状态 |
| Axios | `1.20.0` | Bearer 注入和统一响应解包 |
| Element Plus | `2.14.5` | 按需导入的后台基础组件 |

精确安装结果由 `package-lock.json` 固定；`package.json` 保留同一主版本内的语义化范围。

## 目录与职责

```text
src/
├── core/
│   ├── api/contracts.ts            # code/data/message、用户与菜单类型、业务错误
│   ├── auth/sessionStorage.ts      # 当前浏览器会话的 Bearer token
│   ├── config/runtime.ts            # API 前缀与超时配置
│   └── http/client.ts               # Axios、认证头和统一解包
├── layouts/AppLayout.vue            # 基于已授权菜单渲染的应用壳
├── modules/
│   ├── auth/                        # 登录 API 与登录页
│   └── system/overview/index.vue    # 初始 componentKey 的实际页面入口
├── router/index.ts                  # 动态路由注册、清理和登录守卫
├── stores/
│   ├── app.ts                       # 纯 UI 外壳状态
│   └── session.ts                   # SYSTEM 会话、/auth/me 投影与动态路由生命周期
└── main.ts                          # 先恢复会话和路由，再挂载应用
```

`src/modules/overview/pages/OverviewPage.vue` 是系统概览的展示实现；`src/modules/system/overview/index.vue` 是数据库菜单 `componentKey = system/overview/index` 对应的稳定入口。

## 会话与统一 API 契约

后端所有已注册 API 返回 HTTP `200` 与：

```json
{ "code": 200, "data": {}, "message": "OK" }
```

`src/core/http/client.ts` 在每次请求时从 `sessionStorage` 读取 token 并写入：

```http
Authorization: Bearer <accessToken>
```

响应中 `code !== 200` 时，请求层抛出 `ApiBusinessError`；认证相关状态码为 `401` 或 `403`。后端的 `CommonApiCode` 统一维护框架默认 code/message，业务模块也可返回自己的数值结果码，因此前端响应契约接受任意数值 `code`，并仅用 `API_CODE` 保存当前通用判断所需的已知常量。页面和 Store 不直接解析 Axios 原始响应，也不把 HTTP 200 误认为业务成功。

令牌键为 `pipker.system.access-token`，只存于当前浏览器会话。刷新页面时，`main.ts` 先请求 `/api/auth/me`：

- 请求成功：Pinia 写入用户、角色、权限和菜单，并注册动态路由。
- 会话失效：清理 token、授权状态和动态路由，进入登录页。
- 登录成功：同样先完成 `/api/auth/me` 和路由注册，再导航到原请求路径或第一个授权页面。

## 数据库菜单与组件键

`/api/auth/me` 的菜单树提供 `type`、`path`、`routeName`、`componentKey`、`permission` 与 `children`。路由器只处理满足下列条件的 `MENU` 节点：

```text
type === MENU
path 非空
routeName 非空
componentKey 非空
```

路由器使用：

```ts
import.meta.glob('../modules/**/index.vue')
```

将 `componentKey` 映射为 `src/modules/<componentKey>.vue`。例如：

```text
system/overview/index  →  src/modules/system/overview/index.vue
```

未找到对应组件的菜单不会被伪造成静态页面：它会被跳过，并只在开发环境输出诊断。退出登录或授权刷新时，前一份数据库菜单注册的路由会被移除。要新增页面，应先增加 Vue 组件，再通过后端的 Liquibase 增量 changeset 增加菜单、角色菜单关系和所需权限；本期不提供管理端 CRUD 页面。

`AppLayout` 仅渲染会话 Store 中的菜单，不再硬编码“系统概览”导航。

## 开发 Route Manifest

当后端显式设置 `pipker.dev.route-manifest.enabled=true` 时，匿名 `GET /api/_dev/routes` 返回当前菜单的最小路由投影：`path`、`name`、`componentKey`、`permission`。它是开发工具辅助接口，不是前端路由的来源；生产默认关闭，关闭时后端未注册该 Controller，访问结果是 HTTP 404。

## 本地运行与构建

先按根目录 [README](../README.md) 与后端 README 配置并启动后端数据库 Profile。前端开发服务器默认把 `/api` 代理到 `http://localhost:8080`；可在 `.env.example` 所示的 `VITE_API_BASE_URL`、`VITE_HTTP_TIMEOUT_MS` 和 `PIPKER_DEV_BACKEND_ORIGIN` 中按环境覆盖公开运行时配置。

```bash
cd frontend
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

部署时，反向代理需要转发 `/api` 到后端，并为 Vue Router 的 History 模式将未知**前端**路径回退到 `index.html`。后端关闭的 `/api/_dev/routes` 仍应转发，让它保持服务端真实的 404 语义。
