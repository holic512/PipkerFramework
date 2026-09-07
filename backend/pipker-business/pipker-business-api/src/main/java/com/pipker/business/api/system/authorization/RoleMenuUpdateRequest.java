/**
 * @file RoleMenuUpdateRequest.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Defines the full-replacement request body for one ordinary role's page-menu assignments.
 * @logic An empty list intentionally removes every page menu from the selected role; item validity is enforced by the service against the database.
 * @dependencies Jakarta Validation, Java Standard Library
 * @index_tags rbac, role-menu, request-validation
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * 角色页面菜单全量更新请求。
 */
public record RoleMenuUpdateRequest(
        @NotNull(message = "menuIds must not be null")
        List<@NotNull(message = "menuIds must not contain null") @Positive(message = "menuIds must be positive") Long> menuIds
) {
}
