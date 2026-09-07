/**
 * @file SystemRoleMenuAssignment.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Represents one persisted role-to-page-menu assignment.
 * @logic The pair is used to build the role-route configuration response and never carries API authorization data.
 * @dependencies Java Standard Library
 * @index_tags rbac, role-menu, assignment
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

/**
 * 角色菜单关联投影。
 */
public record SystemRoleMenuAssignment(Long roleId, Long menuId) {
}
