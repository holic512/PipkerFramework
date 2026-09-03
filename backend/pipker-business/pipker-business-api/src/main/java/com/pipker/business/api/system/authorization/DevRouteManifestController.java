/**
 * @file DevRouteManifestController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 在显式开发配置开启时暴露数据库动态路由的最小 Manifest。
 * @logic Controller 由 ConditionalOnProperty 整体控制注册，开启后只查询菜单路由并返回 path/name/componentKey/permission。
 * @dependencies SystemAuthorizationService、Spring Boot、Spring Web MVC
 * @index_tags controller、dev、route-manifest
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.common.api.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 开发辅助动态路由 Manifest Controller。
 */
@RestController
@RequestMapping("/api/_dev/routes")
@ConditionalOnProperty(prefix = "pipker.dev.route-manifest", name = "enabled", havingValue = "true")
public class DevRouteManifestController {

    private final SystemAuthorizationService systemAuthorizationService;

    /**
     * 创建开发路由 Controller。
     *
     * @param systemAuthorizationService 系统授权服务
     */
    public DevRouteManifestController(SystemAuthorizationService systemAuthorizationService) {
        this.systemAuthorizationService = systemAuthorizationService;
    }

    /**
     * 返回数据库当前动态页面路由。
     *
     * @return 路由列表包装对象
     */
    @GetMapping
    public ApiResponse<Map<String, List<SystemRouteManifestItem>>> routes() {
        return ApiResponse.success(Map.of("routes", systemAuthorizationService.findRouteManifest()));
    }
}
