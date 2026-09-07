/**
 * @file SystemRole.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Represents a system role when configuring role-owned page menus.
 * @logic Keeps role identity and lifecycle state separate from API permissions and menu assignments.
 * @dependencies Java Standard Library
 * @index_tags rbac, role, role-menu
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

/**
 * 系统角色持久化投影。
 */
public record SystemRole(
        Long id,
        String roleCode,
        String roleName,
        String status,
        Integer sort
) {

    /**
     * 判断角色是否处于启用状态。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
