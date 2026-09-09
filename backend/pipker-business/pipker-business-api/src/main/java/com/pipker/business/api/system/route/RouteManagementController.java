/**
 * @file RouteManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统已落库路由定义的只读分页与详情 HTTP API。
 * @logic 控制器类以粗粒度路由查看权限保护所有只读端点；控制器只接受查询条件并委托只读服务，不暴露新增、编辑或删除操作。
 * @dependencies Permission、PermissionEnum、RouteManagementService、RouteManagementResponse、ApiResponse、Spring Web MVC
 * @index_tags controller、route、permission、system-menu、pagination、read-only、administration
 * @author holic512
 */
package com.pipker.business.api.system.route;

import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.api.system.route.RouteManagementResponse.PageResult;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteDetail;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteSummary;
import com.pipker.business.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /** 查看单条路由或目录的完整落库定义。 */
    @GetMapping("/{routeId}")
    public ApiResponse<RouteDetail> routeDetail(@PathVariable String routeId) {
        return ApiResponse.success(routeManagementService.findRouteDetail(routeId));
    }
}
