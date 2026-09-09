/**
 * @file AuthController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Provides system-user login and current authorization projection endpoints.
 * @logic Login is explicitly anonymous; the protected current-user endpoint requires the coarse authorization-view permission and resolves the cached role, enum-permission, route and menu snapshot.
 * @dependencies Permission、PermissionEnum、SystemAuthenticationService, CurrentSystemAuthorizationService, Spring Web MVC
 * @index_tags controller, auth, rbac, permission, cache
 * @author holic512
 */
package com.pipker.business.api.system.auth;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.api.system.auth.dto.LoginRequest;
import com.pipker.business.api.system.auth.dto.LoginResponse;
import com.pipker.business.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统认证 Controller。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SystemAuthenticationService systemAuthenticationService;
    private final CurrentSystemAuthorizationService currentSystemAuthorizationService;

    /**
     * 创建认证 Controller。
     *
     * @param systemAuthenticationService 系统认证服务
     * @param currentSystemAuthorizationService 当前授权服务
     */
    public AuthController(
            SystemAuthenticationService systemAuthenticationService,
            CurrentSystemAuthorizationService currentSystemAuthorizationService
    ) {
        this.systemAuthenticationService = systemAuthenticationService;
        this.currentSystemAuthorizationService = currentSystemAuthorizationService;
    }

    /**
     * 使用 system_user 凭证创建 Bearer 会话。
     *
     * @param request 登录凭证
     * @return 令牌和公开用户资料
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(systemAuthenticationService.login(request));
    }

    /**
     * 返回当前用户实时角色、权限和菜单。
     *
     * @return 当前授权快照
     */
    @GetMapping("/me")
    @Permission(PermissionEnum.SYSTEM_AUTHORIZATION_VIEW)
    public ApiResponse<SystemAuthorizationSnapshot> me() {
        return ApiResponse.success(currentSystemAuthorizationService.currentSnapshot());
    }
}
