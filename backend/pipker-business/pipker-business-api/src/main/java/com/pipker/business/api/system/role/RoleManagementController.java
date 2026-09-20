/**
 * @file RoleManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统角色的分页筛选、增删改、批量处理、页面与接口权限、权限缓存刷新、成员和密码重置 HTTP API。
 * @logic 控制器类以 PermissionEnum.SYSTEM_ROLE_MANAGE 统一声明接口权限；页面与接口权限写入分别维护对应授权来源，缓存刷新端点只重建当前应用实例的角色接口权限映射。
 * @dependencies Permission、PermissionEnum、RoleManagementService、ApiResponse、Spring Web MVC、Jakarta Validation
 * @index_tags controller、rbac、role、route、permission、cache、password-reset、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.role;

import com.pipker.business.api.system.role.RoleManagementRequest.BatchDelete;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchStatus;
import com.pipker.business.api.system.role.RoleManagementRequest.Create;
import com.pipker.business.api.system.role.RoleManagementRequest.ResetMemberPassword;
import com.pipker.business.api.system.role.RoleManagementRequest.ReplacePermissions;
import com.pipker.business.api.system.role.RoleManagementRequest.ReplaceRoutes;
import com.pipker.business.api.system.role.RoleManagementRequest.Update;
import com.pipker.business.api.system.role.RoleManagementResponse.OperationResult;
import com.pipker.business.api.system.role.RoleManagementResponse.PageResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleDetail;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleMember;
import com.pipker.business.api.system.role.RoleManagementResponse.RolePermissionConfiguration;
import com.pipker.business.api.system.role.RoleManagementResponse.RolePermissionCacheRefreshResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RolePermissionUpdateResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleRouteConfiguration;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleRouteUpdateResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleSummary;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.starter.log.annotation.ApiLog;
import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统角色管理 Controller。 */
@RestController
@RequestMapping("/api/admin/roles")
@Permission(PermissionEnum.SYSTEM_ROLE_MANAGE)
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    /** 创建角色管理 Controller。 */
    public RoleManagementController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    /** 分页筛选角色。 */
    @GetMapping
    @ApiLog(purpose = "分页筛选系统角色")
    public ApiResponse<PageResult<RoleSummary>> rolePage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(roleManagementService.findRolePage(page, pageSize, keyword, status));
    }

    /** 创建角色。 */
    @PostMapping
    @ApiLog(purpose = "创建系统角色")
    public ApiResponse<RoleSummary> createRole(@Valid @RequestBody Create request) {
        return ApiResponse.success(roleManagementService.createRole(request));
    }

    /** 查询角色详情。 */
    @GetMapping("/{roleId}")
    @ApiLog(purpose = "查询系统角色详情")
    public ApiResponse<RoleDetail> roleDetail(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.findRoleDetail(roleId));
    }

    /** 编辑角色。 */
    @PutMapping("/{roleId}")
    @ApiLog(purpose = "更新系统角色")
    public ApiResponse<RoleSummary> updateRole(
            @PathVariable String roleId,
            @Valid @RequestBody Update request
    ) {
        return ApiResponse.success(roleManagementService.updateRole(roleId, request));
    }

    /** 删除单个普通角色。 */
    @DeleteMapping("/{roleId}")
    @ApiLog(purpose = "删除系统角色")
    public ApiResponse<OperationResult> deleteRole(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.deleteRoles(new BatchDelete(List.of(roleId))));
    }

    /** 查询一个角色的页面访问权限和可配置路由树。 */
    @GetMapping("/{roleId}/routes")
    @ApiLog(purpose = "查询角色页面权限")
    public ApiResponse<RoleRouteConfiguration> routeConfiguration(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.findRoleRouteConfiguration(roleId));
    }

    /** 全量替换一个普通角色的页面访问权限。 */
    @PutMapping("/{roleId}/routes")
    @ApiLog(purpose = "替换角色页面权限")
    public ApiResponse<RoleRouteUpdateResult> replaceRouteConfiguration(
            @PathVariable String roleId,
            @Valid @RequestBody ReplaceRoutes request
    ) {
        return ApiResponse.success(roleManagementService.replaceRoleRouteConfiguration(roleId, request));
    }

    /** 查询一个角色保存的接口权限和历史失效权限。 */
    @GetMapping("/{roleId}/permissions")
    @ApiLog(purpose = "查询角色接口权限")
    public ApiResponse<RolePermissionConfiguration> permissionConfiguration(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.findRolePermissionConfiguration(roleId));
    }

    /** 覆盖一个普通启用角色的当前有效接口权限。 */
    @PutMapping("/{roleId}/permissions")
    @ApiLog(purpose = "替换角色接口权限")
    public ApiResponse<RolePermissionUpdateResult> replacePermissionConfiguration(
            @PathVariable String roleId,
            @Valid @RequestBody ReplacePermissions request
    ) {
        return ApiResponse.success(roleManagementService.replaceRolePermissionConfiguration(roleId, request));
    }

    /** 手动刷新当前应用实例的角色接口权限一级缓存。 */
    @PostMapping("/permission-cache/refresh")
    @ApiLog(purpose = "刷新角色接口权限缓存")
    public ApiResponse<RolePermissionCacheRefreshResult> refreshPermissionCache() {
        return ApiResponse.success(roleManagementService.refreshRolePermissionCache());
    }

    /** 批量变更普通角色状态。 */
    @PatchMapping("/batch-status")
    @ApiLog(purpose = "批量变更系统角色状态")
    public ApiResponse<OperationResult> batchUpdateStatus(@Valid @RequestBody BatchStatus request) {
        return ApiResponse.success(roleManagementService.updateRoleStatus(request));
    }

    /** 批量删除普通角色。 */
    @PostMapping("/batch-delete")
    @ApiLog(purpose = "批量删除系统角色")
    public ApiResponse<OperationResult> batchDelete(@Valid @RequestBody BatchDelete request) {
        return ApiResponse.success(roleManagementService.deleteRoles(request));
    }

    /** 分页筛选一个角色下的成员账户。 */
    @GetMapping("/{roleId}/members")
    @ApiLog(purpose = "分页查询角色成员")
    public ApiResponse<PageResult<RoleMember>> memberPage(
            @PathVariable String roleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(roleManagementService.findMemberPage(roleId, page, pageSize, keyword));
    }

    /** 重置一个角色成员账户的密码。 */
    @PutMapping("/{roleId}/members/{userId}/password")
    @ApiLog(purpose = "重置角色成员密码")
    public ApiResponse<OperationResult> resetMemberPassword(
            @PathVariable String roleId,
            @PathVariable String userId,
            @Valid @RequestBody ResetMemberPassword request
    ) {
        return ApiResponse.success(roleManagementService.resetMemberPassword(roleId, userId, request));
    }
}
