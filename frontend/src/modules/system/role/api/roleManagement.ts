/**
 * @file roleManagement.ts
 * @project Pipker Framework
 * @module Frontend Role Management API
 * @description Defines typed requests for system role lifecycle, page and interface permission configuration, local permission-cache refresh, member pagination, member password resets, and the shared enum permission catalog export.
 * @logic Keeps Snowflake IDs as strings end-to-end and routes role-owned requests through the shared authenticated HTTP client; permission-cache refresh targets only the application instance handling the request.
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、rbac、role、route、permission、cache、pagination、password-reset
 * @author holic512
 */

import type {
  PageResult,
  RoleManagementOperationResult,
  RolePermissionCacheRefreshResult,
  RolePermissionConfiguration,
  RolePermissionUpdateResult,
  RoleRouteConfiguration,
  RoleRouteUpdateResult,
  SystemRoleDetail,
  SystemRoleMember,
  SystemRoleStatus,
  SystemRoleSummary,
} from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export { getPermissionPoints } from '../../permission/api/permissionManagement'

export interface RolePageQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: SystemRoleStatus
}

export interface RoleMemberPageQuery {
  page: number
  pageSize: number
  keyword?: string
}

export interface CreateRolePayload {
  roleCode: string
  roleName: string
  description: string | null
  status: SystemRoleStatus
  sort: number
}

export interface UpdateRolePayload {
  roleName: string
  description: string | null
  status: SystemRoleStatus
  sort: number
}

export function getRolePage(query: RolePageQuery): Promise<PageResult<SystemRoleSummary>> {
  return requestApi<PageResult<SystemRoleSummary>>({
    method: 'GET',
    url: '/admin/roles',
    params: query,
  })
}

export function createRole(payload: CreateRolePayload): Promise<SystemRoleSummary> {
  return requestApi<SystemRoleSummary>({
    method: 'POST',
    url: '/admin/roles',
    data: payload,
  })
}

export function getRoleDetail(roleId: string): Promise<SystemRoleDetail> {
  return requestApi<SystemRoleDetail>({
    method: 'GET',
    url: `/admin/roles/${roleId}`,
  })
}

export function updateRole(roleId: string, payload: UpdateRolePayload): Promise<SystemRoleSummary> {
  return requestApi<SystemRoleSummary>({
    method: 'PUT',
    url: `/admin/roles/${roleId}`,
    data: payload,
  })
}

export function deleteRole(roleId: string): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'DELETE',
    url: `/admin/roles/${roleId}`,
  })
}

/** 读取一个角色的可访问页面与完整启用路由树。 */
export function getRoleRouteConfiguration(roleId: string): Promise<RoleRouteConfiguration> {
  return requestApi<RoleRouteConfiguration>({
    method: 'GET',
    url: `/admin/roles/${encodeURIComponent(roleId)}/routes`,
  })
}

/** 全量覆盖一个普通角色的页面访问权限。 */
export function replaceRoleRouteConfiguration(
  roleId: string,
  routeIds: string[],
): Promise<RoleRouteUpdateResult> {
  return requestApi<RoleRouteUpdateResult>({
    method: 'PUT',
    url: `/admin/roles/${encodeURIComponent(roleId)}/routes`,
    data: { routeIds },
  })
}

/** 读取一个角色持有的当前有效接口权限与历史失效权限。 */
export function getRolePermissionConfiguration(roleId: string): Promise<RolePermissionConfiguration> {
  return requestApi<RolePermissionConfiguration>({
    method: 'GET',
    url: `/admin/roles/${encodeURIComponent(roleId)}/permissions`,
  })
}

/** 全量覆盖一个普通启用角色的当前有效接口权限。 */
export function replaceRolePermissionConfiguration(
  roleId: string,
  permissionCodes: string[],
): Promise<RolePermissionUpdateResult> {
  return requestApi<RolePermissionUpdateResult>({
    method: 'PUT',
    url: `/admin/roles/${encodeURIComponent(roleId)}/permissions`,
    data: { permissionCodes },
  })
}

/** 全量刷新当前应用实例的角色接口权限一级缓存。 */
export function refreshRolePermissionCache(): Promise<RolePermissionCacheRefreshResult> {
  return requestApi<RolePermissionCacheRefreshResult>({
    method: 'POST',
    url: '/admin/roles/permission-cache/refresh',
  })
}

export function batchUpdateRoleStatus(
  roleIds: string[],
  status: SystemRoleStatus,
): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'PATCH',
    url: '/admin/roles/batch-status',
    data: { roleIds, status },
  })
}

export function batchDeleteRoles(roleIds: string[]): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'POST',
    url: '/admin/roles/batch-delete',
    data: { roleIds },
  })
}

export function getRoleMemberPage(
  roleId: string,
  query: RoleMemberPageQuery,
): Promise<PageResult<SystemRoleMember>> {
  return requestApi<PageResult<SystemRoleMember>>({
    method: 'GET',
    url: `/admin/roles/${roleId}/members`,
    params: query,
  })
}

export function resetRoleMemberPassword(
  roleId: string,
  userId: string,
  newPassword: string,
): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'PUT',
    url: `/admin/roles/${roleId}/members/${userId}/password`,
    data: { newPassword },
  })
}
