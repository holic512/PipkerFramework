/**
 * @file RouteManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统路由分页、树形、详情及受限配置维护 API。
 * @logic 查询受查看权限保护，完整管理树支持任意目录层级；配置更新以方法级管理权限保护，不提供路由新增和删除。
 * @dependencies Permission、PermissionEnum、RouteManagementService、RouteManagementResponse、ApiResponse、Spring Web MVC
 * @index_tags controller、route、permission、system-menu、pagination、tree、read-only、administration
 * @author holic512
 */
package com.pipker.business.api.system.route;

import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.api.system.route.RouteManagementResponse.PageResult;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteDetail;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteSummary;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteTreeNode;
import com.pipker.business.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统路由管理 Controller。 */
@RestController
@RequestMapping("/api/admin/routes")
@Permission(PermissionEnum.SYSTEM_ROUTE_VIEW)
public class RouteManagementController {

    private final RouteManagementService routeManagementService;

    /** 创建路由管理 Controller。 */
    public RouteManagementController(RouteManagementService routeManagementService) {
        this.routeManagementService = routeManagementService;
    }

    /** 分页筛选所有数据库路由与目录定义。 */
    @GetMapping
    public ApiResponse<PageResult<RouteSummary>> routePage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String menuType,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(routeManagementService.findRoutePage(
                page,
                pageSize,
                keyword,
                menuType,
                visible,
                status
        ));
    }

    /** 返回完整的目录—页面管理树，支持任意深度的目录嵌套。 */
    @GetMapping("/tree")
    public ApiResponse<List<RouteTreeNode>> routeTree() {
        return ApiResponse.success(routeManagementService.findRouteTree());
    }

    /** 更新展示配置，不改变源码对应的路由结构。 */
    @PutMapping("/{routeId}/configuration")
    @Permission(PermissionEnum.SYSTEM_ROUTE_MANAGE)
    public ApiResponse<RouteDetail> updateConfiguration(
            @PathVariable String routeId, @RequestBody RouteConfigurationRequest request
    ) {
        return ApiResponse.success(routeManagementService.updateConfiguration(routeId, request));
    }

    /** 查看单条路由或目录的完整落库定义。 */
    @GetMapping("/{routeId}")
    public ApiResponse<RouteDetail> routeDetail(@PathVariable String routeId) {
        return ApiResponse.success(routeManagementService.findRouteDetail(routeId));
    }
}
