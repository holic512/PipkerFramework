/**
 * @file contracts.ts
 * @project Pipker Framework
 * @module Frontend API Contracts
 * @description Defines the server envelope, RBAC projections, and typed error surface shared by frontend features.
 * @logic Keeps all backend field names and business result codes at the transport boundary so pages only consume verified data.
 * @dependencies TypeScript
 * @index_tags api, contracts, rbac, authentication
 * @author holic512
 */

export const API_CODE = {
  SUCCESS: 200,
  VALIDATION_FAILED: 400,
  AUTH_INVALID_CREDENTIALS: 401,
  AUTH_ACCOUNT_DISABLED: 403,
  AUTH_REQUIRED: 401,
  AUTH_FORBIDDEN: 403,
  INTERNAL_ERROR: 500,
} as const

export type ApiCode = number

export interface ApiResponse<T> {
  code: ApiCode
  data: T | null
  message: string
}

export class ApiBusinessError extends Error {
  readonly code: ApiCode

  constructor(code: ApiCode, message: string) {
    super(message)
    this.name = 'ApiBusinessError'
    this.code = code
  }
}

export interface SystemUserProfile {
  id: number
  username: string
  nickname: string | null
  avatar: string | null
}

export interface SystemMenuNode {
  id: number
  parentId: number | null
  name: string
  type: 'DIRECTORY' | 'MENU' | 'BUTTON'
  path: string | null
  routeName: string | null
  componentKey: string | null
  icon: string | null
  sort: number | null
  permission: string | null
  children: SystemMenuNode[]
}

export interface SystemAuthorizationSnapshot {
  user: SystemUserProfile
  roles: string[]
  permissions: string[]
  menus: SystemMenuNode[]
}

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  user: SystemUserProfile
}

export interface LoginRequest {
  username: string
  password: string
}
