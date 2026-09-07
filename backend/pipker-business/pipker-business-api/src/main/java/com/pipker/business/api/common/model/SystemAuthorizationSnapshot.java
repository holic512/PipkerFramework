/**
 * @file SystemAuthorizationSnapshot.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Aggregates a system user's cached database-derived roles, permissions, and page-menu tree.
 * @logic Current-user endpoints and API-filter authorization share the same projection source while the cache owns its bounded refresh interval.
 * @dependencies SystemUserProfile, SystemMenuNode, Java Standard Library
 * @index_tags rbac, authorization, api, cache
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
        List<SystemMenuNode> menus
) {
}
