/**
 * @file roleMenu.ts
 * @project Pipker Framework
 * @module Frontend Role Route API
 * @description Defines the administration requests for reading and replacing role-owned page menu assignments.
 * @logic Uses the shared HTTP client so database API-resource authorization and the standard response envelope remain the only backend security boundary.
 * @dependencies requestApi, RoleMenuConfiguration, RoleMenuUpdateResult
 * @index_tags api, rbac, role-menu, administration
 * @author holic512
 */

import type { RoleMenuConfiguration, RoleMenuUpdateResult } from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export function getRoleMenuConfiguration(): Promise<RoleMenuConfiguration> {
  return requestApi<RoleMenuConfiguration>({
    method: 'GET',
    url: '/admin/role-menu-config',
  })
}

export function replaceRoleMenuAssignments(roleId: number, menuIds: number[]): Promise<RoleMenuUpdateResult> {
  return requestApi<RoleMenuUpdateResult>({
    method: 'PUT',
    url: `/admin/role-menu-config/${roleId}`,
    data: { menuIds },
  })
}
