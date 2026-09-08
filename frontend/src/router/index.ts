/**
 * @file index.ts
 * @project Pipker Framework
 * @module Frontend Router
 * @description Registers the public home selector and login routes alongside the protected layout, then derives protected page routes from /api/auth/me authorized route definitions.
 * @logic Keeps / publicly reachable, resolves componentKey through import.meta.glob, registers hidden-but-authorized pages without showing them in navigation, and removes stale routes on session cleanup.
 * @dependencies Vue Router, AppLayout, LoginPage, public home selector, sessionStorage, SystemRouteDefinition
 * @index_tags router, homepage, dynamic-routing, rbac, route, authentication
 * @author holic512
 */

import { createRouter, createWebHistory } from 'vue-router'
import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { SystemRouteDefinition } from '../core/api/contracts'
import { readAccessToken } from '../core/auth/sessionStorage'
import AppLayout from '../layouts/AppLayout.vue'
import LoginPage from '../modules/auth/pages/LoginPage.vue'

const APP_LAYOUT_ROUTE_NAME = 'app-layout'
const HOME_ROUTE_NAME = 'home'
const LOGIN_ROUTE_NAME = 'login'
const pageModules = import.meta.glob<Component>('../modules/**/index.vue')
const databaseRouteNames = new Set<string>()
const authorizedRouteIds = new Set<string>()
let defaultAuthorizedPath: string | null = null

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: HOME_ROUTE_NAME,
    component: () => import('../modules/home/index.vue'),
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
  const requiredRouteId = typeof to.meta.routeId === 'string' ? to.meta.routeId : null
  if (hasAccessToken && requiredRouteId !== null && !authorizedRouteIds.has(requiredRouteId)) {
    return defaultAuthorizedPath ?? { name: APP_LAYOUT_ROUTE_NAME }
  }
  return true
})

router.afterEach((to) => {
  const pageTitle = typeof to.meta.title === 'string' ? to.meta.title : '管理控制台'
  document.title = `${pageTitle} · Pipker Framework`
})

export function replaceDatabaseRoutes(
  routes: SystemRouteDefinition[],
  defaultVisiblePath: string | null,
): void {
  clearDatabaseRoutes()
  for (const route of routes) {
    const componentModulePath = `../modules/${route.componentKey}.vue`
    const component = pageModules[componentModulePath]
    if (!component) {
      if (import.meta.env.DEV) {
        console.warn(
          `[pipker] Skipping route "${route.routeName}" because componentKey "${route.componentKey}" has no matching src/modules/**/index.vue file.`,
        )
      }
      continue
    }
    if (router.hasRoute(route.routeName)) {
      if (import.meta.env.DEV) {
        console.warn(`[pipker] Skipping duplicate database route name "${route.routeName}".`)
      }
      continue
    }

    router.addRoute(APP_LAYOUT_ROUTE_NAME, {
      path: toChildPath(route.path),
      name: route.routeName,
      component,
      meta: {
        title: route.title,
        routeId: route.id,
        componentKey: route.componentKey,
      },
    })
    databaseRouteNames.add(route.routeName)
    authorizedRouteIds.add(route.id)
    if (defaultVisiblePath === route.path) {
      defaultAuthorizedPath = route.path
    } else {
      defaultAuthorizedPath ??= route.path
    }
  }
}

export function clearDatabaseRoutes(): void {
  for (const routeName of databaseRouteNames) {
    router.removeRoute(routeName)
  }
  databaseRouteNames.clear()
  authorizedRouteIds.clear()
  defaultAuthorizedPath = null
}

export function getDefaultAuthorizedPath(): string | null {
  return defaultAuthorizedPath
}

function toChildPath(path: string): string {
  return path.replace(/^\/+/, '')
}
