/**
 * @file permissionManagement.ts
 * @project Pipker Framework
 * @module Frontend Permission Point API
 * @description Defines the read-only request for the Java PermissionEnum interface-permission catalog.
 * @logic Keeps the permission-point catalog request in its own system module so the directory page and role permission dialog share one transport definition.
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、permission、permission-point、rbac、read-only
 * @author holic512
 */

import type { SystemPermissionPoint } from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

/** 读取当前 Java PermissionEnum 定义的全部有效接口权限点。 */
export function getPermissionPoints(): Promise<SystemPermissionPoint[]> {
  return requestApi<SystemPermissionPoint[]>({
    method: 'GET',
    url: '/admin/permissions',
  })
}
