/**
 * 文件：index.ts
 * 项目：Pipker Framework
 * 模块：路由组装
 * 说明：将业务模块路由组装到共享应用布局下。
 * 处理逻辑：使用浏览器 History 路由，在导航后更新文档标题，并将未匹配路径重定向到概览页面。
 * 依赖：Vue Router、概览模块路由、应用布局
 * 检索关键词：前端、路由、路由组装、布局
 * 作者：holic512
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import { overviewRoutes } from '../modules/overview/routes'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: overviewRoutes,
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : '管理控制台'
  document.title = `${pageTitle} · Pipker Framework`
})
