/**
 * @file AdminAuthorizationController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Provides the administrative authorization projection endpoint.
 * @logic Declares the coarse authorization-view permission and returns the same cached enum-permission, route and menu snapshot used by the current-session endpoint.
 * @dependencies Permission、PermissionEnum、CurrentSystemAuthorizationService, Spring Web MVC
 * @index_tags controller, admin, rbac, permission, authorization
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台授权读取 Controller。
 */
@RestController
@RequestMapping("/api/admin/authorization")
public class AdminAuthorizationController {

    private final CurrentSystemAuthorizationService currentSystemAuthorizationService;

    /**
     * 创建 Controller。
     *
     * @param currentSystemAuthorizationService 当前授权服务
     */
    public AdminAuthorizationController(CurrentSystemAuthorizationService currentSystemAuthorizationService) {
        this.currentSystemAuthorizationService = currentSystemAuthorizationService;
    }

    /**
     * 读取当前后台用户的授权快照。
     *
     * @return 当前授权快照
     */
    @GetMapping
    @Permission(PermissionEnum.SYSTEM_AUTHORIZATION_VIEW)
    public ApiResponse<SystemAuthorizationSnapshot> authorization() {
        return ApiResponse.success(currentSystemAuthorizationService.currentSnapshot());
    }
}
