/**
 * @file RouteConfigurationRequest.java
 * @project Pipker Framework
 * @module 系统路由管理
 * @description 限定后台可维护的路由展示与启用配置。
 * @logic 仅接收五个配置字段；路由结构由 Liquibase 管理，服务层统一校验。
 * @dependencies Java 标准库
 * @index_tags route、configuration、request
 * @author holic512
 */
package com.pipker.business.api.system.route;

public record RouteConfigurationRequest(
        String menuName, String icon, Integer sort, Boolean visible, String status
) {
}
