/**
 * @file SystemRouteDefinition.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 承载已授权页面菜单的前端动态路由注册信息。
 * @logic 将菜单展示树与可访问页面路由解耦：隐藏菜单仍可作为已授权路由注册，目录节点从不进入该投影。
 * @dependencies Java 标准库
 * @index_tags route、rbac、dynamic-routing、read-model
 * @author holic512
 */
package com.pipker.business.api.common.model;

/** 已授权页面路由的安全读模型。 */
public record SystemRouteDefinition(
        String id,
        String title,
        String path,
        String routeName,
        String componentKey
) {
}
