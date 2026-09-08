/**
 * @file index.ts
 * @project Pipker Framework
 * @module Frontend Router
 * @description Registers the public home and login routes alongside the protected layout, then derives protected page routes exclusively from /api/auth/me menu data.
 * @logic Keeps / publicly reachable, resolves componentKey through import.meta.glob, removes stale routes on session cleanup, and redirects the protected layout root to the first authorized page.
 * @dependencies Vue Router, AppLayout, LoginPage, HomePage, sessionStorage, SystemMenuNode
 * @index_tags router, homepage, dynamic-routing, rbac, authentication
 * @author holic512
 */

import { createRouter, createWebHistory } from 'vue-router'
import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { SystemMenuNode } from '../core/api/contracts'
import { readAccessToken } from '../core/auth/sessionStorage'
import AppLayout from '../layouts/AppLayout.vue'
import LoginPage from '../modules/auth/pages/LoginPage.vue'

const APP_LAYOUT_ROUTE_NAME = 'app-layout'
const HOME_ROUTE_NAME = 'home'
const LOGIN_ROUTE_NAME = 'login'
const pageModules = import.meta.glob<Component>('../modules/**/index.vue')
const databaseRouteNames = new Set<string>()
const authorizedPageMenuIds = new Set<number>()
let defaultAuthorizedPath: string | null = null

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: HOME_ROUTE_NAME,
    component: () => import('../modules/home/pages/HomePage.vue'),
    meta: { title: '系统首页', public: true },
  },
  {
    path: '/login',
    name: LOGIN_ROUTE_NAME,
    component: LoginPage,
    meta: { title: '系统登录', public: true },
  },
  {
    path: '/',
    name: APP_LAYOUT_ROUTE_NAME,
    component: AppLayout,
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: () => (readAccessToken()
      ? { name: APP_LAYOUT_ROUTE_NAME, params: {} }
      : { name: LOGIN_ROUTE_NAME, params: {} }),
  },
]

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const hasAccessToken = readAccessToken() !== null
  if (!hasAccessToken && to.name !== LOGIN_ROUTE_NAME && to.name !== HOME_ROUTE_NAME) {
    return {
      name: LOGIN_ROUTE_NAME,
      query: to.fullPath === '/' ? undefined : { redirect: to.fullPath },
    }
  }
  if (hasAccessToken && to.name === LOGIN_ROUTE_NAME) {
    return defaultAuthorizedPath ?? { name: APP_LAYOUT_ROUTE_NAME }
  }
  if (hasAccessToken && to.name === APP_LAYOUT_ROUTE_NAME && defaultAuthorizedPath) {
    return defaultAuthorizedPath
  }
  const requiredMenuId = typeof to.meta.menuId === 'number' ? to.meta.menuId : null
  if (hasAccessToken && requiredMenuId !== null && !authorizedPageMenuIds.has(requiredMenuId)) {
    return defaultAuthorizedPath ?? { name: APP_LAYOUT_ROUTE_NAME }
  }
  return true
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : '管理控制台'
  document.title = `${pageTitle} · Pipker Framework`
})

export function replaceDatabaseRoutes(menus: SystemMenuNode[]): void {
  clearDatabaseRoutes()
  forEachMenu(menus, (menu) => {
    if (!isPageMenu(menu)) {
      return
    }

    const componentModulePath = `../modules/${menu.componentKey}.vue`
    const component = pageModules[componentModulePath]
    if (!component) {
      if (import.meta.env.DEV) {
        console.warn(
          `[pipker] Skipping route "${menu.routeName}" because componentKey "${menu.componentKey}" has no matching src/modules/**/index.vue file.`,
        )
      }
      return
    }
    if (router.hasRoute(menu.routeName)) {
      if (import.meta.env.DEV) {
        console.warn(`[pipker] Skipping duplicate database route name "${menu.routeName}".`)
      }
      return
    }

    router.addRoute(APP_LAYOUT_ROUTE_NAME, {
      path: toChildPath(menu.path),
      name: menu.routeName,
      component,
      meta: {
        title: menu.name,
        menuId: menu.id,
        componentKey: menu.componentKey,
      },
    })
    databaseRouteNames.add(menu.routeName)
    authorizedPageMenuIds.add(menu.id)
    defaultAuthorizedPath ??= menu.path
  })
}

export function clearDatabaseRoutes(): void {
  for (const routeName of databaseRouteNames) {
    router.removeRoute(routeName)
  }
  databaseRouteNames.clear()
  authorizedPageMenuIds.clear()
  defaultAuthorizedPath = null
}

export function getDefaultAuthorizedPath(): string | null {
  return defaultAuthorizedPath
}

function forEachMenu(menus: SystemMenuNode[], visitor: (menu: SystemMenuNode) => void): void {
  for (const menu of menus) {
    visitor(menu)
    forEachMenu(menu.children, visitor)
  }
}

function isPageMenu(menu: SystemMenuNode): menu is SystemMenuNode & {
  type: 'MENU'
  path: string
  routeName: string
  componentKey: string
} {
  return menu.type === 'MENU'
    && typeof menu.path === 'string'
    && menu.path.length > 0
    && typeof menu.routeName === 'string'
    && menu.routeName.length > 0
    && typeof menu.componentKey === 'string'
    && menu.componentKey.length > 0
}

function toChildPath(path: string): string {
  return path.replace(/^\/+/, '')
}
