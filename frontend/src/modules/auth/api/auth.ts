/**
 * @file auth.ts
 * @project Pipker Framework
 * @module Frontend Authentication API
 * @description Defines the frontend requests for system login and the current database-backed authorization projection.
 * @logic Sends credentials only to the anonymous login endpoint, then fetches the authenticated /auth/me projection as the route source of truth.
 * @dependencies requestApi, LoginRequest, LoginResponse, SystemAuthorizationSnapshot
 * @index_tags api, authentication, authorization, rbac
 * @author holic512
 */

import type {
  LoginRequest,
  LoginResponse,
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
