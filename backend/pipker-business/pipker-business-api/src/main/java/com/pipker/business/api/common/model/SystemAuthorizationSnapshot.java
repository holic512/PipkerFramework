/**
 * @file SystemAuthorizationSnapshot.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Aggregates a system user's cached database-derived roles, permissions, display-menu tree, and authorized page-route definitions.
 * @logic Current-user endpoints and API-filter authorization share one projection source; visible menus drive navigation while all authorized page routes drive dynamic registration.
 * @dependencies SystemUserProfile, SystemMenuNode, SystemRouteDefinition, Java Standard Library
 * @index_tags rbac, authorization, api, cache, route
 * @author holic512
 */
package com.pipker.business.api.common.model;

import java.util.List;

/**
 * 用户授权信息快照。
 */
public record SystemAuthorizationSnapshot(
        SystemUserProfile user,
        List<String> roles,
        List<String> permissions,
        List<SystemMenuNode> menus,
        List<SystemRouteDefinition> routes
) {
}
