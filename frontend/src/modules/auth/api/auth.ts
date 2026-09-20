/**
 * @file auth.ts
 * @project Pipker Framework
 * @module Frontend Authentication API
 * @description Defines frontend requests for system login, current-token logout and the current database-backed authorization projection.
 * @logic Sends credentials only to the anonymous login endpoint, invalidates the active Bearer token through the protected logout endpoint, and uses /auth/me as the route source of truth.
 * @dependencies requestApi、LoginRequest、LoginResponse、LogoutResponse、SystemAuthorizationSnapshot
 * @index_tags api、authentication、authorization、logout、rbac
 * @author holic512
 */

import type {
  LoginRequest,
  LoginResponse,
  LogoutResponse,
  SystemAuthorizationSnapshot,
} from '../../../core/api/contracts'
import { requestApi } from '../../../core/http/client'

export function loginSystemUser(payload: LoginRequest): Promise<LoginResponse> {
  return requestApi<LoginResponse>({
    method: 'POST',
    url: '/auth/login',
    data: payload,
  })
}

export function getCurrentAuthorization(): Promise<SystemAuthorizationSnapshot> {
  return requestApi<SystemAuthorizationSnapshot>({
    method: 'GET',
    url: '/auth/me',
  })
}

export function logoutSystemUser(): Promise<LogoutResponse> {
  return requestApi<LogoutResponse>({
    method: 'POST',
    url: '/auth/logout',
  })
}
