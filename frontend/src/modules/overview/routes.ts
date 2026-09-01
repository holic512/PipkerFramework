/**
 * 文件：routes.ts
 * 项目：Pipker Framework
 * 模块：概览模块路由
 * 说明：暴露框架概览业务模块负责的路由。
 * 处理逻辑：将页面归属保留在模块内部，同时允许中央路由器将其组装到共享布局下。
 * 依赖：Vue Router、OverviewPage
 * 检索关键词：前端、模块、概览、路由
 * 作者：holic512
 */
import type { RouteRecordRaw } from 'vue-router'

export const overviewRoutes: RouteRecordRaw[] = [
  {
    path: '',
    name: 'overview',
    component: () => import('./pages/OverviewPage.vue'),
    meta: {
      title: '系统概览',
    },
  },
]
