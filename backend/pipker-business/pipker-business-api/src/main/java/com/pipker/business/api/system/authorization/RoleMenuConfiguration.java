/**
 * @file RoleMenuConfiguration.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Defines the complete role-route configuration payload consumed by the administration page.
 * @logic Returns enabled roles with their editable state and one canonical menu tree so the client saves only selected leaf menu IDs.
 * @dependencies SystemMenuNode, Java Standard Library
 * @index_tags rbac, role-menu, api-contract
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.model.SystemMenuNode;

import java.util.List;

/**
 * 角色路由配置页面的数据载荷。
 */
public record RoleMenuConfiguration(
        List<RoleMenuConfigurationRole> roles,
        List<SystemMenuNode> menus
) {

    /**
     * 可配置角色及其当前页面菜单选择。
     */
    public record RoleMenuConfigurationRole(
            Long id,
            String code,
            String name,
            Integer sort,
            boolean allMenus,
            List<Long> menuIds
    ) {
    }
}
