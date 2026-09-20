/**
 * @file ApiLogManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供管理员接口访问概览、接口聚合、原始日志分页和单条详情的只读 API。
 * @logic 所有入口统一受接口日志查看权限保护，将 ISO 时间、筛选和分页参数交给分析服务，不提供删除、导出或配置修改能力。
 * @dependencies ApiLog、Permission、ApiLogManagementService、Spring Web MVC
 * @index_tags controller、api-log、observability、analytics、administration
 * @author holic512
 */
package com.pipker.business.api.system.apilog;

import com.pipker.business.api.system.apilog.ApiLogManagementResponse.EndpointSummary;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.LogDetail;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.LogSummary;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.Overview;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.PageResult;
import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.starter.log.annotation.ApiLog;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 接口访问日志管理 Controller。 */
@RestController
@RequestMapping("/api/admin/api-logs")
@Permission(PermissionEnum.SYSTEM_API_LOG_VIEW)
public class ApiLogManagementController {

    private final ApiLogManagementService service;

    public ApiLogManagementController(ApiLogManagementService service) {
        this.service = service;
    }

    /** 查询接口访问概览和趋势。 */
    @GetMapping("/overview")
    @ApiLog(purpose = "查询接口访问概览和趋势")
    public ApiResponse<Overview> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long slowThresholdMs,
            @RequestParam(defaultValue = "false") boolean includeMonitorEndpoints
    ) {
        return ApiResponse.success(service.overview(startTime, endTime, slowThresholdMs, includeMonitorEndpoints));
    }

    /** 分页查询接口维度性能聚合。 */
    @GetMapping("/endpoints")
    @ApiLog(purpose = "分页分析接口性能和成功率")
    public ApiResponse<PageResult<EndpointSummary>> endpointPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long slowThresholdMs,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "AVG_DURATION") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "false") boolean includeMonitorEndpoints
    ) {
        return ApiResponse.success(service.endpointPage(
                page, pageSize, startTime, endTime, slowThresholdMs,
                keyword, sortBy, sortDirection, includeMonitorEndpoints
        ));
    }

    /** 分页筛选接口访问明细。 */
    @GetMapping
    @ApiLog(purpose = "分页筛选接口访问明细")
    public ApiResponse<PageResult<LogSummary>> logPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Long minDurationMs,
            @RequestParam(required = false) String routePattern,
            @RequestParam(required = false) Boolean unresolvedRoute
    ) {
        return ApiResponse.success(service.logPage(
                page, pageSize, startTime, endTime, keyword, httpMethod, success, minDurationMs,
                routePattern, unresolvedRoute
        ));
    }

    /** 查询一条接口访问日志详情。 */
    @GetMapping("/{logId}")
    @ApiLog(purpose = "查询接口访问日志详情")
    public ApiResponse<LogDetail> detail(@PathVariable String logId) {
        return ApiResponse.success(service.detail(logId));
    }
}
