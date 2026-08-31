# Pipker Framework

Pipker Framework 以独立的后端 Maven Reactor 与前端 Vue 应用组成：

```text
PipkerFramework/
├── backend/   # Spring Boot：Starter 基础设施、业务模块、Server 组装层
└── frontend/  # Vue：core 平台能力、业务模块、路由与页面布局
```

- 后端模块与依赖方向见 [backend/README.md](backend/README.md)。
- 前端分层、运行方式与依赖版本见 [frontend/README.md](frontend/README.md)。
