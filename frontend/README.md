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
| Sass | `1.104.0` | 全局设计 token、复用模式与页面样式编译 |

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
├── components/ThemeSwitcher.vue      # 设计语言与明暗模式切换器
├── modules/
│   ├── auth/                        # 登录 API 与登录页
│   ├── home/                        # 公开首页选择器、默认基线与系统专属定制边界
│   │   ├── index.vue                # / 路由入口；根据源码开关选择默认或定制首页
│   │   ├── api/config.ts            # 默认页 / 定制页的唯一源码选择配置
│   │   └── pages/
│   │       ├── defaults/            # 可拆分的框架默认公开首页
│   │       └── custom/              # 当前系统的私有公开首页
│   └── system/overview/index.vue    # 初始 componentKey 的实际页面入口
├── router/index.ts                  # 动态路由注册、清理和登录守卫
├── stores/
│   ├── app.ts                       # 纯 UI 外壳状态
│   ├── session.ts                   # SYSTEM 会话、/auth/me 投影与动态路由生命周期
│   └── theme.ts                     # 设计语言与浅色/深色选择及本地持久化
├── styles/
│   ├── _abstracts.scss               # Sass 字体、断点、函数与无输出 mixin
│   ├── _themes.scss                  # Claude / Element × 浅色 / 深色 token map
│   ├── _element-plus.scss            # Element Plus CSS 变量语义映射
│   ├── _patterns.scss                # 重置、页面、面板、标题和切换器复用样式
│   └── index.scss                    # 唯一全局 Sass 样式入口
└── main.ts                          # 先恢复会话和路由，再挂载应用
```

`src/modules/overview/pages/OverviewPage.vue` 是系统概览的展示实现；`src/modules/system/overview/index.vue` 是数据库菜单 `componentKey = system/overview/index` 对应的稳定入口。

## 公开首页

`/` 是不要求登录的系统介绍页，展示 Pipker 的技术基线、授权路由机制与交付边界。根路由加载 `src/modules/home/index.vue`；该入口只选择当前要渲染的首页组件，不进行重定向，因此 URL 始终保持为 `/`，也不会污染浏览器历史或改变匿名访问规则。

首页页面统一位于 `src/modules/home/pages/`，并分为两个明确边界：

- `src/modules/home/pages/defaults/` 是框架默认公开首页。它是可继续拆分的长页基线，包含导航、Hero、CSS 授权流程预览、能力说明、收束 CTA 与页脚。
- `src/modules/home/pages/custom/` 是当前系统的专属定制区。

`src/modules/home/api/config.ts` 公开唯一选择入口 `isCustomHomeEnabled`。将其设为 `true` 后，`/` 会直接渲染 `pages/custom/index.vue` 的项目私有首页；设为 `false` 则渲染框架默认页，整个过程不产生额外公开路由。当前启用状态以该源码配置文件为唯一事实来源。

根首页选择器统一提供控制台跳转基线：未登录用户进入 `/login`，已恢复有效会话的用户进入 `/api/auth/me` 返回菜单中的第一个已授权页面。`api/` 当前只保存首页变体选择配置；公开首页不注册业务页面、不提供菜单数据，也不会绕过登录后的数据库 RBAC 动态路由机制。

## 主题与 Sass 设计系统

前端默认使用 **Claude + 浅色** 组合，并完整支持下面四种组合：

| 设计语言 | 浅色 | 深色 | 设计取向 |
| --- | --- | --- | --- |
| Claude | Claude Light | Claude Dark | 单色后台：暖灰白与墨黑层级、细分隔线、浅色侧栏与深色当前项 |
| Element | Element Light | Element Dark | 中性后台基底、Element 蓝、紧凑控件与轻边框 |

所有页面和 Element Plus 控件都使用 `MiSans, "PingFang SC", -apple-system, "system-ui", "Helvetica Neue", Helvetica, Arial, "Hiragino Sans GB", "Microsoft YaHei", sans-serif` 字体栈。不会请求 Google Fonts 或依赖展示字体、等宽字体的网络资源。

主题状态由 `src/stores/theme.ts` 的 `useThemeStore` 管理。它公开 `DesignTheme`（`claude | element`）、`ColorScheme`（`light | dark`）以及 `initializeTheme()`、`setDesignTheme()`、`setColorScheme()`、`toggleColorScheme()`。选择以单个 `localStorage` 对象 `pipker.ui.theme-preference` 持久化；初次访问且没有有效偏好时回退到 Claude 浅色。

应用启动时会在恢复会话、挂载页面之前调用 `initializeTheme()`。Store 会同步根元素，便于 CSS 与 Element Plus 共同响应：

```html
<html data-design-theme="claude|element" data-color-scheme="light|dark" class="dark">
```

`dark` class 只会在深色模式存在，符合 Element Plus 的暗色变量启用方式。公开首页、登录页以及登录后的应用壳顶栏均放置共享的 `ThemeSwitcher`，任一入口切换后会即时影响当前应用的所有页面。

全局入口是 `src/styles/index.scss`，按“主题 token → Element Plus 映射 → 基础与复用模式”的顺序加载。Vite 会向每个 `lang="scss"` 页面注入 `src/styles/_abstracts.scss`，因此页面可使用 `@include at-most('phone')`、`@include panel()` 等无输出 mixin，同时将通用 `ui-page-frame`、`ui-panel`、`ui-eyebrow`、`ui-code` 和焦点态留在全局模式层，避免重复编写。

标准表面统一采用低圆角尺度：`--radius-small = 4px`、`--radius-control = 6px`、`--radius-panel = 8px`。只有语义上确实需要连续形状的技术标签、数量徽标使用 `--radius-pill`，而状态点与图形装饰保留 `50%` 圆形；页面不得额外写入大圆角值。

新增设计语言或色彩模式时，只需在 `src/styles/_themes.scss` 中补充一组完整的语义 token map（页面/表面、文本、边框、主色、状态色、侧栏、布局尺寸、圆角、阴影等），再将其登记到 `$themes`。页面不应复制四份颜色规则，也不应新增页面级固定色值；应优先使用 `--color-*`、`--radius-*`、`--shadow-*` 与布局语义变量。Claude 的状态色也必须停留在中性灰阶，避免组件库默认的蓝、绿、黄、红破坏单色规则。Element Plus 组件颜色统一通过 `_element-plus.scss` 的 `--el-*` 映射继承。

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

`/api/auth/me` 的菜单树提供 `id`、`type`、`path`、`routeName`、`componentKey` 与 `children`。菜单来自当前用户全部启用角色的页面菜单关联。路由器只处理满足下列条件的 `MENU` 节点：

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

未找到对应组件的菜单不会被伪造成静态页面：它会被跳过，并只在开发环境输出诊断。退出登录或授权刷新时，前一份数据库菜单注册的路由会被移除；导航守卫还会检查路由的菜单 ID 是否仍在当前授权菜单集合中。要新增页面，应先增加 Vue 组件，再通过后端的 Liquibase 增量 changeset 增加菜单并为角色配置菜单关联；页面调用 API 所需权限仍通过独立 API 权限配置。`SUPER_ADMIN` 可通过“角色路由”页面为普通角色勾选页面菜单，本期不实现按钮级权限控制。

`AppLayout` 仅渲染会话 Store 中的菜单，不再硬编码“系统概览”导航。后端不再提供开发 Route Manifest；页面数据唯一来自已登录用户的 `/api/auth/me`。

## 本地运行与构建

先按根目录 [README](../README.md) 与后端 README 配置并启动后端数据库 Profile。前端开发服务器默认把 `/api` 和公开文件路径 `/files` 代理到 `http://localhost:8080`；后端返回的文件引用保持 `/files/...` 根相对路径，浏览器会按当前开发域名访问。可在 `.env.example` 所示的 `VITE_API_BASE_URL`、`VITE_HTTP_TIMEOUT_MS` 和 `PIPKER_DEV_BACKEND_ORIGIN` 中按环境覆盖公开运行时配置。

```bash
cd frontend
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

部署时，反向代理需要转发 `/api` 和 `/files/**` 到后端，并为 Vue Router 的 History 模式将未知**前端**路径回退到 `index.html`。
