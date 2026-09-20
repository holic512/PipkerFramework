/**
 * @file LoginLogManagementController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供管理员认证生命周期日志只读分页查询 API。
 * @logic 使用独立登录日志查看权限保护接口，将 ISO 本地时间、筛选条件和分页参数委托给查询服务，不提供修改、删除、导出或清理能力。
 * @dependencies Permission、PermissionEnum、LoginLogManagementService、Spring Web MVC
 * @index_tags controller、authentication、audit、login-log、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.loginlog;

import com.pipker.business.api.system.loginlog.LoginLogManagementResponse.LoginLogSummary;
import com.pipker.business.api.system.loginlog.LoginLogManagementResponse.PageResult;
import com.pipker.business.api.system.permission.Permission;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** 登录日志管理 Controller。 */
@RestController
@RequestMapping("/api/admin/login-logs")
@Permission(PermissionEnum.SYSTEM_LOGIN_LOG_VIEW)
public class LoginLogManagementController {

    private final LoginLogManagementService loginLogManagementService;

    /** 创建登录日志查询 Controller。 */
    public LoginLogManagementController(LoginLogManagementService loginLogManagementService) {
        this.loginLogManagementService = loginLogManagementService;
    }

    /** 分页筛选认证生命周期日志。 */
    @GetMapping
    public ApiResponse<PageResult<LoginLogSummary>> loginLogPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        return ApiResponse.success(loginLogManagementService.findPage(
                page, pageSize, keyword, eventType, result, startTime, endTime
        ));
    }
}
