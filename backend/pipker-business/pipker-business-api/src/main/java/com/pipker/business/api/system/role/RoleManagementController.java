/**
 * @file RoleManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统角色的分页筛选、增删改、批量处理、详情、页面权限、成员与密码重置 HTTP API。
 * @logic 所有端点由数据库 API 资源规则统一鉴权；页面权限端点委托同一角色菜单关联服务以确保路由守卫和菜单投影一致。
 * @dependencies RoleManagementService、ApiResponse、Spring Web MVC、Jakarta Validation
 * @index_tags controller、rbac、role、route、password-reset、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.role;

import com.pipker.business.api.system.role.RoleManagementRequest.BatchDelete;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchStatus;
import com.pipker.business.api.system.role.RoleManagementRequest.Create;
import com.pipker.business.api.system.role.RoleManagementRequest.ResetMemberPassword;
import com.pipker.business.api.system.role.RoleManagementRequest.ReplaceRoutes;
import com.pipker.business.api.system.role.RoleManagementRequest.Update;
import com.pipker.business.api.system.role.RoleManagementResponse.OperationResult;
import com.pipker.business.api.system.role.RoleManagementResponse.PageResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleDetail;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleMember;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleRouteConfiguration;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleRouteUpdateResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleSummary;
import com.pipker.business.common.api.ApiResponse;
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
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    /** 创建角色管理 Controller。 */
    public RoleManagementController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    /** 分页筛选角色。 */
    @GetMapping
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
    public ApiResponse<RoleSummary> createRole(@Valid @RequestBody Create request) {
        return ApiResponse.success(roleManagementService.createRole(request));
    }

    /** 查询角色详情。 */
    @GetMapping("/{roleId}")
    public ApiResponse<RoleDetail> roleDetail(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.findRoleDetail(roleId));
    }

    /** 编辑角色。 */
    @PutMapping("/{roleId}")
    public ApiResponse<RoleSummary> updateRole(
            @PathVariable String roleId,
            @Valid @RequestBody Update request
    ) {
        return ApiResponse.success(roleManagementService.updateRole(roleId, request));
    }

    /** 删除单个普通角色。 */
    @DeleteMapping("/{roleId}")
    public ApiResponse<OperationResult> deleteRole(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.deleteRoles(new BatchDelete(List.of(roleId))));
    }

    /** 查询一个角色的页面访问权限和可配置路由树。 */
    @GetMapping("/{roleId}/routes")
    public ApiResponse<RoleRouteConfiguration> routeConfiguration(@PathVariable String roleId) {
        return ApiResponse.success(roleManagementService.findRoleRouteConfiguration(roleId));
    }

    /** 全量替换一个普通角色的页面访问权限。 */
    @PutMapping("/{roleId}/routes")
    public ApiResponse<RoleRouteUpdateResult> replaceRouteConfiguration(
            @PathVariable String roleId,
            @Valid @RequestBody ReplaceRoutes request
    ) {
        return ApiResponse.success(roleManagementService.replaceRoleRouteConfiguration(roleId, request));
    }

    /** 批量变更普通角色状态。 */
    @PatchMapping("/batch-status")
    public ApiResponse<OperationResult> batchUpdateStatus(@Valid @RequestBody BatchStatus request) {
        return ApiResponse.success(roleManagementService.updateRoleStatus(request));
    }

    /** 批量删除普通角色。 */
    @PostMapping("/batch-delete")
    public ApiResponse<OperationResult> batchDelete(@Valid @RequestBody BatchDelete request) {
        return ApiResponse.success(roleManagementService.deleteRoles(request));
    }

    /** 分页筛选一个角色下的成员账户。 */
    @GetMapping("/{roleId}/members")
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
    public ApiResponse<OperationResult> resetMemberPassword(
            @PathVariable String roleId,
            @PathVariable String userId,
            @Valid @RequestBody ResetMemberPassword request
    ) {
        return ApiResponse.success(roleManagementService.resetMemberPassword(roleId, userId, request));
    }
}
