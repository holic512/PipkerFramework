/**
 * @file session.ts
 * @project Pipker Framework
 * @module Frontend Session State
 * @description Owns the active SYSTEM session, its authorization projection, server-side current-token logout, and the dynamic route lifecycle.
 * @logic Persists only the Bearer token; login and refresh fetch /auth/me, while logout first requests server invalidation and always clears local state and routes in a finally block.
 * @dependencies Pinia、Vue、authentication API、router、sessionStorage、notification、API contracts
 * @index_tags pinia、authentication、logout、rbac、route、dynamic-routing
 * @author holic512
 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { API_CODE, ApiBusinessError } from '../core/api/contracts'
import type {
  ApiCode,
  LoginRequest,
  SystemAuthorizationSnapshot,
  SystemMenuNode,
  SystemRouteDefinition,
  SystemUserProfile,
} from '../core/api/contracts'
import {
  clearAccessToken,
  readAccessToken,
  type SessionPersistence,
  writeAccessToken,
} from '../core/auth/sessionStorage'
import { clearDatabaseRoutes, router, replaceDatabaseRoutes } from '../router'
import { notification } from '../core/notification'
import { getCurrentAuthorization, loginSystemUser, logoutSystemUser } from '../modules/auth/api/auth'

export const useSessionStore = defineStore('session', () => {
  const accessToken = ref(readAccessToken())
  const user = ref<SystemUserProfile | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<SystemMenuNode[]>([])
  const routes = ref<SystemRouteDefinition[]>([])
  const restoring = ref(false)

  const isAuthenticated = computed(() => accessToken.value !== null && user.value !== null)

  function applyAuthorization(snapshot: SystemAuthorizationSnapshot): void {
    user.value = snapshot.user
    roles.value = snapshot.roles
    permissions.value = snapshot.permissions
    menus.value = snapshot.menus
    routes.value = snapshot.routes
    replaceDatabaseRoutes(snapshot.routes, findFirstVisiblePagePath(snapshot.menus))
  }

  function clearSession(): void {
    clearAccessToken()
    accessToken.value = null
    user.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    routes.value = []
    clearDatabaseRoutes()
  }

  async function refreshAuthorization(): Promise<SystemAuthorizationSnapshot> {
    const snapshot = await getCurrentAuthorization()
    applyAuthorization(snapshot)
    return snapshot
  }

  async function login(
    credentials: LoginRequest,
    persistence: SessionPersistence = 'session',
  ): Promise<SystemAuthorizationSnapshot> {
    const result = await loginSystemUser({
      username: credentials.username.trim(),
      password: credentials.password,
    })
    writeAccessToken(result.accessToken, persistence)
    accessToken.value = result.accessToken
    return refreshAuthorization()
  }

  async function restoreSession(): Promise<boolean> {
    if (!accessToken.value) {
      return false
    }

    restoring.value = true
    try {
      await refreshAuthorization()
      return true
    } catch (error) {
      if (isAuthenticationFailure(error)) {
        clearSession()
        return false
      }
      throw error
    } finally {
      restoring.value = false
    }
  }

  async function logout(): Promise<void> {
    let serverLogoutFailed = false
    try {
      if (accessToken.value) {
        await logoutSystemUser()
      }
    } catch {
      serverLogoutFailed = true
    } finally {
      clearSession()
      await router.replace({ name: 'login' })
    }
    if (serverLogoutFailed) {
      notification.warning('服务端会话注销失败，已清除本地登录信息。')
    }
  }

  return {
    accessToken,
    user,
    roles,
    permissions,
    menus,
    routes,
    restoring,
    isAuthenticated,
    login,
    refreshAuthorization,
    restoreSession,
    logout,
    clearSession,
  }
})

function findFirstVisiblePagePath(menus: SystemMenuNode[]): string | null {
  for (const menu of menus) {
    if (menu.type === 'MENU' && menu.path) {
      return menu.path
    }
    const childPath = findFirstVisiblePagePath(menu.children)
    if (childPath) {
      return childPath
    }
  }
  return null
}

function isAuthenticationFailure(error: unknown): boolean {
  if (!(error instanceof ApiBusinessError)) {
    return false
  }
  const invalidSessionCodes: ApiCode[] = [
    API_CODE.AUTH_REQUIRED,
    API_CODE.AUTH_INVALID_CREDENTIALS,
    API_CODE.AUTH_ACCOUNT_DISABLED,
  ]
  return invalidSessionCodes.includes(error.code)
}
