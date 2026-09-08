/**
 * @file RoleMenuConfiguration.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Defines role-route configuration payloads consumed by administration screens.
 * @logic Returns canonical menu trees and per-role editable state, serializing every snowflake ID as a string so callers save exact page IDs without losing precision.
 * @dependencies SystemMenuNode, Java Standard Library
 * @index_tags rbac, role-menu, route, api-contract, snowflake-id
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
            String id,
            String code,
            String name,
            String status,
            Integer sort,
            boolean allMenus,
            List<String> menuIds
    ) {
    }
}
