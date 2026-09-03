/**
 * @file SystemRouteManifestItem.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义开发辅助 Route Manifest 对外暴露的最小路由字段。
 * @logic 只投影动态菜单的路径、名称、组件键与权限，不携带任何账户或基础设施信息。
 * @dependencies Java 标准库
 * @index_tags dev、route-manifest、system-menu
 * @author holic512
 */
package com.pipker.business.api.system.model;

/**
 * Route Manifest 路由项。
 */
public record SystemRouteManifestItem(
        String path,
        String name,
        String componentKey,
        String permission
) {
}
