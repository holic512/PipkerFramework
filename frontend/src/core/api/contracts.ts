/**
 * @file contracts.ts
 * @project Pipker Framework
 * @module Frontend API Contracts
 * @description Defines the server envelope, authentication lifecycle, enum permissions, user and role projections, login logs, and route-management contracts shared by frontend features.
 * @logic Keeps backend field names and business codes at the transport boundary, including string Snowflake IDs, current-token logout, immutable authentication audit records, recursive routes, and memory-defined permissions.
 * @dependencies TypeScript
 * @index_tags api, contracts, rbac, user, role, route, permission, authorization, authentication, login-log
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

export interface LoginResponse {
  accessToken: string
  tokenType: 'Bearer'
  user: SystemUserProfile
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LogoutResponse {
  loggedOut: boolean
}

export type AuthenticationEventType = 'LOGIN' | 'LOGOUT' | 'AUTH_CHECK'
export type AuthenticationEventResult = 'SUCCESS' | 'FAILURE'
export type AuthenticationFailureReason =
  | 'INVALID_CREDENTIALS'
  | 'ACCOUNT_DISABLED'
  | 'AUTH_REQUIRED'
  | 'AUTH_FORBIDDEN'
  | 'VALIDATION_FAILED'
  | 'INTERNAL_ERROR'

export interface SystemLoginLogSummary {
  id: string
  userId: string | null
  username: string | null
  eventType: AuthenticationEventType
  result: AuthenticationEventResult
  failureReason: AuthenticationFailureReason | null
  clientIp: string | null
  userAgent: string | null
  httpMethod: string | null
  requestPath: string | null
  traceId: string | null
  occurredAt: string
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

/** Java 权限枚举在管理端展示的有效接口权限点。 */
export interface SystemPermissionPoint {
  code: string
  name: string
  description: string
}

/** 一个角色的当前有效接口权限与仅供识别的历史失效权限。 */
export interface RolePermissionConfiguration {
  roleId: string
  roleCode: string
  roleName: string
  status: SystemRoleStatus
  allPermissions: boolean
  permissionCodes: string[]
  historicalPermissionCodes: string[]
}

export interface RolePermissionUpdateResult {
  roleId: string
  permissionCodes: string[]
}

/** 当前应用实例的角色接口权限一级缓存刷新结果。 */
export interface RolePermissionCacheRefreshResult {
  refreshedAt: string
  enabledRoleCount: number
  effectivePermissionGrantCount: number
}

export type SystemUserStatus = 'ENABLED' | 'DISABLED'

export interface SystemUserRoleAssignment {
  id: string
  roleCode: string
  roleName: string
  status: SystemRoleStatus
}

export interface SystemUserSummary {
  id: string
  username: string
  nickname: string | null
  phone: string | null
  email: string | null
  status: SystemUserStatus
  lastLoginTime: string | null
  createdAt: string
  roles: SystemUserRoleAssignment[]
  protectedAccount: boolean
}

export interface SystemUserDetail extends SystemUserSummary {
  updatedAt: string
}

export interface AssignableSystemRole {
  id: string
  roleCode: string
  roleName: string
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

export interface SystemRouteTreeNode extends SystemRouteSummary {
  children: SystemRouteTreeNode[]
}

export interface SystemRouteDetail extends SystemRouteSummary {
  parentName: string | null
  createdAt: string
  updatedAt: string
}
