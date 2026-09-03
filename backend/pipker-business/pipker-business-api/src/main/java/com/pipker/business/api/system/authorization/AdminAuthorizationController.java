/**
 * @file AdminAuthorizationController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供受 system:authorization:read 保护的后台授权读取基线接口。
 * @logic 直接调用 Sa-Token 权限检查后返回同一份数据库实时授权快照，为后续 /api/admin/** 管理接口建立约定。
 * @dependencies Sa-Token、CurrentSystemAuthorizationService、Spring Web MVC
 * @index_tags controller、admin、rbac
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import cn.dev33.satoken.stp.StpUtil;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
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

    /**
     * 后台授权读取权限。
     */
    public static final String AUTHORIZATION_READ_PERMISSION = "system:authorization:read";

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
    public ApiResponse<SystemAuthorizationSnapshot> authorization() {
        StpUtil.checkPermission(AUTHORIZATION_READ_PERMISSION);
        return ApiResponse.success(currentSystemAuthorizationService.currentSnapshot());
    }
}
