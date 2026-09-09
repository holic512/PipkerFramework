/**
 * @file userManagement.ts
 * @project Pipker Framework
 * @module Frontend User Management API
 * @description 定义系统用户筛选、详情、创建、更新、角色分配、状态维护、删除和密码重置的类型化请求。
 * @logic 所有雪花主键以字符串传递，用户资料与完整角色分配由同一更新请求写入，密码仅作为一次性命令负载发送。
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、user、rbac、role-assignment、password-reset、pagination
 * @author holic512
 */

import type {
  AssignableSystemRole,
  PageResult,
  RoleManagementOperationResult,
  SystemUserDetail,
  SystemUserStatus,
  SystemUserSummary,
} from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export interface UserPageQuery {
  page: number
  pageSize: number
  keyword?: string
  status?: SystemUserStatus
}

export interface CreateUserPayload {
  username: string
  password: string
  nickname: string | null
  phone: string | null
  email: string | null
  status: SystemUserStatus
  roleIds: string[]
}

export interface UpdateUserPayload {
  nickname: string | null
  phone: string | null
  email: string | null
  status: SystemUserStatus
  roleIds: string[]
}

export function getUserPage(query: UserPageQuery): Promise<PageResult<SystemUserSummary>> {
  return requestApi<PageResult<SystemUserSummary>>({
    method: 'GET',
    url: '/admin/users',
    params: query,
  })
}

export function getUserDetail(userId: string): Promise<SystemUserDetail> {
  return requestApi<SystemUserDetail>({
    method: 'GET',
    url: `/admin/users/${encodeURIComponent(userId)}`,
  })
}

export function getAssignableRoles(): Promise<AssignableSystemRole[]> {
  return requestApi<AssignableSystemRole[]>({
    method: 'GET',
    url: '/admin/users/assignable-roles',
  })
}

export function createUser(payload: CreateUserPayload): Promise<SystemUserSummary> {
  return requestApi<SystemUserSummary>({
    method: 'POST',
    url: '/admin/users',
    data: payload,
  })
}

export function updateUser(userId: string, payload: UpdateUserPayload): Promise<SystemUserSummary> {
  return requestApi<SystemUserSummary>({
    method: 'PUT',
    url: `/admin/users/${encodeURIComponent(userId)}`,
    data: payload,
  })
}

export function deleteUser(userId: string): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'DELETE',
    url: `/admin/users/${encodeURIComponent(userId)}`,
  })
}

export function resetUserPassword(userId: string, newPassword: string): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'PUT',
    url: `/admin/users/${encodeURIComponent(userId)}/password`,
    data: { newPassword },
  })
}

export function batchUpdateUserStatus(
  userIds: string[],
  status: SystemUserStatus,
): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'PATCH',
    url: '/admin/users/batch-status',
    data: { userIds, status },
  })
}

export function batchDeleteUsers(userIds: string[]): Promise<RoleManagementOperationResult> {
  return requestApi<RoleManagementOperationResult>({
    method: 'POST',
    url: '/admin/users/batch-delete',
    data: { userIds },
  })
}
