/**
 * @file contracts.ts
 * @project Pipker Framework
 * @module Frontend API Contracts
 * @description Defines the server envelope, RBAC and role-management projections, and typed error surface shared by frontend features.
 * @logic Keeps all backend field names and business result codes at the transport boundary so pages only consume verified data, including string-form Snowflake IDs.
 * @dependencies TypeScript
 * @index_tags api, contracts, rbac, role, authentication
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
  type: 'DIRECTORY' | 'MENU'
  path: string | null
  routeName: string | null
  componentKey: string | null
  icon: string | null
  sort: number | null
  children: SystemMenuNode[]
}

export interface SystemAuthorizationSnapshot {
  user: SystemUserProfile
  roles: string[]
  permissions: string[]
  menus: SystemMenuNode[]
}

export interface RoleMenuConfigurationRole {
  id: number
  code: string
  name: string
  sort: number | null
  allMenus: boolean
  menuIds: number[]
}

export interface RoleMenuConfiguration {
  roles: RoleMenuConfigurationRole[]
  menus: SystemMenuNode[]
}

export interface RoleMenuUpdateResult {
  roleId: number
  menuIds: number[]
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

export type SystemRoleStatus = 'ENABLED' | 'DISABLED'

export interface SystemRoleSummary {
  id: string
  roleCode: string
  roleName: string
  description: string | null
  status: SystemRoleStatus
  sort: number
  createdAt: string
  updatedAt: string
}

export interface SystemRoleDetail extends SystemRoleSummary {
  memberCount: number
}

export interface SystemRoleMember {
  id: string
  username: string
  nickname: string | null
  status: SystemRoleStatus
  lastLoginTime: string | null
  createdAt: string
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  records: T[]
}

export interface RoleManagementOperationResult {
  affectedIds: string[]
}
