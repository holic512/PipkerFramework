/**
 * @file PermissionPointController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供当前 Java 枚举定义的全量有效接口权限点查询接口。
 * @logic 直接返回启动期加载的内存权限目录，不提供分页和数据库权限点增删改；角色管理权限保护此列表以供角色授权弹窗选择。
 * @dependencies Permission、PermissionEnum、PermissionRegistry、ApiResponse、Spring Web MVC
 * @index_tags permission、controller、rbac、api-authorization、administration
 * @author holic512
 */
package com.pipker.business.api.system.permission;

import com.pipker.business.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统权限点读取接口。
 */
@RestController
@RequestMapping("/api/admin/permissions")
@Permission(PermissionEnum.SYSTEM_ROLE_MANAGE)
public class PermissionPointController {

    private final PermissionRegistry permissionRegistry;

    /** 创建权限点读取 Controller。 */
    public PermissionPointController(PermissionRegistry permissionRegistry) {
        this.permissionRegistry = permissionRegistry;
    }

    /** @return 当前 Java 枚举定义的所有有效权限点。 */
    @GetMapping
    public ApiResponse<List<PermissionRegistry.PermissionPoint>> list() {
        return ApiResponse.success(permissionRegistry.list());
    }
}
