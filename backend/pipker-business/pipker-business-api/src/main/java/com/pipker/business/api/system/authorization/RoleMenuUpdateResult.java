/**
 * @file RoleMenuUpdateResult.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Returns the persisted page-menu selection after replacing one role's route assignments.
 * @logic Gives the client a stable save confirmation with exact string-form snowflake IDs, without exposing unrelated roles or API permission data.
 * @dependencies Java Standard Library
 * @index_tags rbac, role-menu, api-contract, snowflake-id
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import java.util.List;

/**
 * 角色页面菜单保存结果。
 */
public record RoleMenuUpdateResult(String roleId, List<String> menuIds) {
}
