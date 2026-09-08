/**
 * @file contracts.ts
 * @project Pipker Framework
 * @module Frontend API Contracts
 * @description Defines the server envelope, RBAC, role-management including page permissions, and read-only route-management projections shared by frontend features.
 * @logic Keeps all backend field names and business result codes at the transport boundary so pages only consume verified data, including string-form Snowflake IDs and the separate navigation/route authorization model.
 * @dependencies TypeScript
 * @index_tags api, contracts, rbac, role, route, authorization, authentication
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
  id: string
  parentId: string | null
  name: string
  type: 'DIRECTORY' | 'MENU'
  path: string | null
  routeName: string | null
  componentKey: string | null
  icon: string | null
  sort: number | null
  visible: boolean
  children: SystemMenuNode[]
}

export interface SystemRouteDefinition {
  id: string
  title: string
  path: string
  routeName: string
  componentKey: string
}

export interface SystemAuthorizationSnapshot {
  user: SystemUserProfile
  roles: string[]
  permissions: string[]
  menus: SystemMenuNode[]
  routes: SystemRouteDefinition[]
}

export interface RoleMenuConfigurationRole {
  id: string
  code: string
  name: string
  status: SystemRoleStatus
  sort: number | null
  allMenus: boolean
  menuIds: string[]
}

export interface RoleMenuConfiguration {
  roles: RoleMenuConfigurationRole[]
  menus: SystemMenuNode[]
}

export interface RoleMenuUpdateResult {
  roleId: string
  menuIds: string[]
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

export interface RoleRouteConfiguration {
  roleId: string
  roleCode: string
  roleName: string
  status: SystemRoleStatus
  allRoutes: boolean
  routeIds: string[]
  routes: SystemMenuNode[]
}

export interface RoleRouteUpdateResult {
  roleId: string
  routeIds: string[]
}

export type SystemRouteMenuType = 'DIRECTORY' | 'MENU'
export type SystemRouteStatus = 'ENABLED' | 'DISABLED'

export interface SystemRouteSummary {
  id: string
  parentId: string | null
  menuName: string
  menuType: SystemRouteMenuType
  routePath: string | null
  routeName: string | null
  componentKey: string | null
  icon: string | null
  sort: number | null
  visible: boolean
  status: SystemRouteStatus
  componentIndexRequired: boolean
}

export interface SystemRouteDetail extends SystemRouteSummary {
  parentName: string | null
  createdAt: string
  updatedAt: string
}
