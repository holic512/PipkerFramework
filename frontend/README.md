# Pipker Framework Frontend

Pipker Framework 的 Vue 3 前端工程。它独立于 `backend/` 构建和部署，通过 HTTP API 与后端交互。

## 技术基线

| 类别 | 已锁定版本 | 用途 |
| --- | --- | --- |
| Vue | `3.5.42` | 视图层与组合式 API |
| TypeScript | `6.0.3` | 应用与接口的静态类型检查 |
| Vite | `8.2.2` | 开发服务器与生产构建 |
| Vue Router | `5.3.0` | 浏览器 History 路由和页面边界 |
| Pinia | `4.0.3` | 跨页面客户端状态 |
| Axios | `1.20.0` | 统一 HTTP 客户端与拦截器扩展点 |
| Element Plus | `2.14.5` | Vue 3 管理端组件库，按需导入 |
| unplugin-vue-components | `32.1.0` | Vite 编译期组件按需导入 |

精确安装结果由 `package-lock.json` 固定；`package.json` 保留语义化版本范围，以便以后显式升级时接收同一主版本内的更新。

Element Plus 通过 Vite 的 `ElementPlusResolver` 按实际使用的组件和样式编译，不在 `main.ts` 全量注册。页面中可直接使用 `el-button`、`el-table` 等模板组件；新增组件会由构建插件自动解析。

## 目录与模块边界

```text
frontend/
├── src/
│   ├── core/                    # 平台能力：运行时配置、HTTP 客户端
│   │   ├── config/
│   │   └── http/
│   ├── router/                  # 路由组装、404 和导航副作用
│   ├── layouts/                 # 跨业务模块的页面骨架
│   ├── modules/                 # 按业务域拆分的前端模块
│   │   └── overview/            # 当前的框架概览模块示例
│   │       ├── pages/
│   │       └── routes.ts
│   ├── stores/                  # 只存放跨模块 UI / 会话状态
│   ├── styles/                  # 全局设计令牌与基础样式
│   ├── App.vue                  # 路由根节点
│   └── main.ts                  # 应用启动和插件注册
├── .env.example                 # 运行时 API 配置样例，不存放密钥
└── vite.config.ts               # Vue 编译与构建配置
```

新增业务功能时，以 `src/modules/<业务域>/` 为边界，优先把该领域的页面、API、局部 Store、类型和路由放在同一个模块中。中央 `router/` 只负责组装模块路由；`core/http/` 只处理传输层共性，不写业务 URL 或响应体规则。跨两个以上模块稳定复用的类型、组件或工具届时再提升到 `src/shared/`，避免过早建立“万能公共层”。这种分层对应后端的“Server 组装层 → Business 模块 → Common/Starter 基础设施”关系：入口负责装配，业务模块保持内聚，平台能力不反向依赖业务页面。

## 请求契约

`src/core/http/client.ts` 通过 `VITE_API_BASE_URL` 创建唯一 Axios 实例，默认前缀为 `/api`。已确认的最小接口是后端 `8080` 端口的 `GET /api/ping`，其返回纯文本；开发服务器会把 `/api` 代理到 `PIPKER_DEV_BACKEND_ORIGIN`（默认 `http://localhost:8080`），概览模块以此做连通性检查。

除该健康检查外，当前仍未发现统一响应包裹、认证、业务错误码或更多业务接口；因此未虚构 `data/code/message` 解包和鉴权拦截器。这些应在相应业务模块的 `api/` 目录中随已确认的接口契约实现。生产环境的反向代理还需同时处理 `/api` 转发，并为 Vue Router 的 History 模式把未知前端路径回退到 `index.html`。

## 运行

```bash
cd frontend
npm run dev
npm run build
```
