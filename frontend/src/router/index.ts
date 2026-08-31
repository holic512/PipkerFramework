/**
 * @file index.ts
 * @project Pipker Framework
 * @module Route composition
 * @description Assembles business-module routes beneath the shared application layout.
 * @logic Uses browser History routing, updates the document title after navigation, and redirects unmatched paths to the overview.
 * @dependencies Vue Router, overview module routes, application layout
 * @index_tags frontend,router,route-composition,layout
 * @author holic512
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
