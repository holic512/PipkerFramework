/**
 * @file SystemApiResource.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Represents one database-declared protected HTTP resource and its required API permission.
 * @logic Stores the request method and Spring MVC path template separately from the permission identity so one permission can protect multiple resources.
 * @dependencies Java Standard Library
 * @index_tags rbac, api-resource, route, permission
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

/**
 * 数据库 API 资源规则的读取模型。
 *
 * @param id 资源主键
 * @param permissionCode 所需 API 权限编码
 * @param httpMethod HTTP 方法
 * @param pathPattern Spring MVC 路径模板
 */
public record SystemApiResource(
        Long id,
        String permissionCode,
        String httpMethod,
        String pathPattern
) {
}
