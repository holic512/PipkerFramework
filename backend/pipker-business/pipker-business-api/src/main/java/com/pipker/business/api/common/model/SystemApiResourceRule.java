/**
 * @file SystemApiResourceRule.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 表示 API 资源与权限定义连接后的只读鉴权规则。
 * @logic 只承载缓存编译路径模板所需字段，不映射任何单独数据库表。
 * @dependencies Java 标准库
 * @index_tags api-authorization、rbac、projection
 * @author holic512
 */
package com.pipker.business.api.common.model;

/** 数据库 API 资源规则的读取投影。 */
public record SystemApiResourceRule(
        Long id,
        String permissionCode,
        String httpMethod,
        String pathPattern
) {
}
