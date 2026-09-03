/**
 * @file AuthController.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 提供系统用户登录和当前授权信息读取接口。
 * @logic 登录由匿名路由放行；当前信息通过 Sa-Token 会话解析并从数据库实时读取 RBAC 数据。
 * @dependencies SystemAuthenticationService、CurrentSystemAuthorizationService、Spring Web MVC
 * @index_tags controller、auth、rbac
 * @author holic512
 */
package com.pipker.server.controller;

import com.pipker.business.api.system.model.SystemAuthorizationSnapshot;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.server.auth.CurrentSystemAuthorizationService;
import com.pipker.server.auth.LoginRequest;
import com.pipker.server.auth.LoginResponse;
import com.pipker.server.auth.SystemAuthenticationService;
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
    public ApiResponse<SystemAuthorizationSnapshot> me() {
        return ApiResponse.success(currentSystemAuthorizationService.currentSnapshot());
    }
}
