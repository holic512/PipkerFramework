/**
 * @file session.ts
 * @project Pipker Framework
 * @module Frontend Session State
 * @description Owns the active SYSTEM session, its authorization projection, and the dynamic route lifecycle.
 * @logic Persists only the Bearer token in sessionStorage; every successful login or refresh fetches /auth/me and synchronizes routes from its menus.
 * @dependencies Pinia, Vue, authentication API, router, sessionStorage, API contracts
 * @index_tags pinia, authentication, rbac, dynamic-routing
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
  SystemUserProfile,
} from '../core/api/contracts'
import {
  clearAccessToken,
  readAccessToken,
  writeAccessToken,
} from '../core/auth/sessionStorage'
import { clearDatabaseRoutes, router, replaceDatabaseRoutes } from '../router'
import { getCurrentAuthorization, loginSystemUser } from '../modules/auth/api/auth'

export const useSessionStore = defineStore('session', () => {
  const accessToken = ref(readAccessToken())
  const user = ref<SystemUserProfile | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<SystemMenuNode[]>([])
  const restoring = ref(false)

  const isAuthenticated = computed(() => accessToken.value !== null && user.value !== null)

  function applyAuthorization(snapshot: SystemAuthorizationSnapshot): void {
    user.value = snapshot.user
    roles.value = snapshot.roles
    permissions.value = snapshot.permissions
    menus.value = snapshot.menus
    replaceDatabaseRoutes(snapshot.menus)
  }

  function clearSession(): void {
    clearAccessToken()
    accessToken.value = null
    user.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    clearDatabaseRoutes()
  }

  async function refreshAuthorization(): Promise<SystemAuthorizationSnapshot> {
    const snapshot = await getCurrentAuthorization()
    applyAuthorization(snapshot)
    return snapshot
  }

  async function login(credentials: LoginRequest): Promise<SystemAuthorizationSnapshot> {
    const result = await loginSystemUser({
      username: credentials.username.trim(),
      password: credentials.password,
    })
    writeAccessToken(result.accessToken)
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
    clearSession()
    await router.replace({ name: 'login' })
  }

  return {
    accessToken,
    user,
    roles,
    permissions,
    menus,
    restoring,
    isAuthenticated,
    login,
    refreshAuthorization,
    restoreSession,
    logout,
    clearSession,
  }
})

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
