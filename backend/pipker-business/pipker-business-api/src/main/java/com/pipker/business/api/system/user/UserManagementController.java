/**
 * @file UserManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统用户的筛选、详情、创建、编辑、角色分配、状态维护、删除和密码重置 HTTP API。
 * @logic 查询端点要求系统用户查看权限，写入端点要求系统用户管理权限；具体的保护账户和角色可分配性规则由服务层统一处理。
 * @dependencies Permission、PermissionEnum、UserManagementService、ApiResponse、Spring Web MVC、Jakarta Validation
 * @index_tags controller、user、rbac、password-reset、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.user;

import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.api.system.user.UserManagementRequest.BatchDelete;
import com.pipker.business.api.system.user.UserManagementRequest.BatchStatus;
import com.pipker.business.api.system.user.UserManagementRequest.Create;
import com.pipker.business.api.system.user.UserManagementRequest.ResetPassword;
import com.pipker.business.api.system.user.UserManagementRequest.Update;
import com.pipker.business.api.system.user.UserManagementResponse.AssignableRole;
import com.pipker.business.api.system.user.UserManagementResponse.OperationResult;
import com.pipker.business.api.system.user.UserManagementResponse.PageResult;
import com.pipker.business.api.system.user.UserManagementResponse.UserDetail;
import com.pipker.business.api.system.user.UserManagementResponse.UserSummary;
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

/** 系统用户管理 Controller。 */
@RestController
@RequestMapping("/api/admin/users")
@Permission(PermissionEnum.SYSTEM_USER_VIEW)
public class UserManagementController {

    private final UserManagementService userManagementService;

    /** 创建用户管理 Controller。 */
    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /** 分页筛选用户。 */
    @GetMapping
    public ApiResponse<PageResult<UserSummary>> userPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(userManagementService.findUserPage(page, pageSize, keyword, status));
    }

    /** 查询用户详情。 */
    @GetMapping("/{userId}")
    public ApiResponse<UserDetail> userDetail(@PathVariable String userId) {
        return ApiResponse.success(userManagementService.findUserDetail(userId));
    }

    /** 返回创建和编辑时可分配的启用普通角色。 */
    @GetMapping("/assignable-roles")
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<List<AssignableRole>> assignableRoles() {
        return ApiResponse.success(userManagementService.findAssignableRoles());
    }

    /** 创建用户。 */
    @PostMapping
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<UserSummary> createUser(@Valid @RequestBody Create request) {
        return ApiResponse.success(userManagementService.createUser(request));
    }

    /** 更新用户资料与角色分配。 */
    @PutMapping("/{userId}")
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<UserSummary> updateUser(@PathVariable String userId, @Valid @RequestBody Update request) {
        return ApiResponse.success(userManagementService.updateUser(userId, request));
    }

    /** 删除一个普通用户。 */
    @DeleteMapping("/{userId}")
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<OperationResult> deleteUser(@PathVariable String userId) {
        return ApiResponse.success(userManagementService.deleteUsers(new BatchDelete(List.of(userId))));
    }

    /** 重置一个普通用户的密码。 */
    @PutMapping("/{userId}/password")
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<OperationResult> resetPassword(
            @PathVariable String userId,
            @Valid @RequestBody ResetPassword request
    ) {
        return ApiResponse.success(userManagementService.resetPassword(userId, request));
    }

    /** 批量变更普通用户状态。 */
    @PatchMapping("/batch-status")
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<OperationResult> batchUpdateStatus(@Valid @RequestBody BatchStatus request) {
        return ApiResponse.success(userManagementService.updateUserStatus(request));
    }

    /** 批量删除普通用户。 */
    @PostMapping("/batch-delete")
    @Permission(PermissionEnum.SYSTEM_USER_MANAGE)
    public ApiResponse<OperationResult> batchDelete(@Valid @RequestBody BatchDelete request) {
        return ApiResponse.success(userManagementService.deleteUsers(request));
    }
}
