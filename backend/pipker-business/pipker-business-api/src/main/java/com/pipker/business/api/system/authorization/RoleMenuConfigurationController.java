/**
 * @file RoleMenuConfigurationController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Exposes the role-first page-route configuration API for the administration page.
 * @logic Delegates read and full-replacement writes to the configuration service; database API-resource rules in the central Sa-Token filter authorize every request.
 * @dependencies RoleMenuConfigurationService, ApiResponse, Spring Web MVC
 * @index_tags controller, rbac, role-menu, administration
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色路由菜单配置 Controller。
 */
@RestController
@RequestMapping("/api/admin/role-menu-config")
public class RoleMenuConfigurationController {

    private final RoleMenuConfigurationService roleMenuConfigurationService;

    /**
     * 创建角色路由菜单配置 Controller。
     */
    public RoleMenuConfigurationController(RoleMenuConfigurationService roleMenuConfigurationService) {
        this.roleMenuConfigurationService = roleMenuConfigurationService;
    }

    /**
     * 返回全部可配置角色和菜单树。
     */
    @GetMapping
    public ApiResponse<RoleMenuConfiguration> configuration() {
        return ApiResponse.success(roleMenuConfigurationService.getConfiguration());
    }

    /**
     * 全量替换一个普通角色的页面菜单。
     */
    @PutMapping("/{roleId}")
    public ApiResponse<RoleMenuUpdateResult> replaceRoleMenus(
            @PathVariable long roleId,
            @Valid @RequestBody RoleMenuUpdateRequest request
    ) {
        return ApiResponse.success(roleMenuConfigurationService.replaceRoleMenuAssignments(
                roleId,
                request.menuIds()
        ));
    }
}
