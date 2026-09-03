/**
 * @file SystemMenu.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 表示 system_menu 中的动态菜单和前端路由定义。
 * @logic 保留逻辑 componentKey 而非源码绝对路径，供前端按需导入匹配组件。
 * @dependencies Java 标准库
 * @index_tags system-menu、route、rbac
 * @author holic512
 */
package com.pipker.business.api.system.model;

import java.time.LocalDateTime;

/**
 * 系统菜单持久化模型。
 */
public record SystemMenu(
        Long id,
        Long parentId,
        String menuName,
        String menuType,
        String routePath,
        String routeName,
        String componentKey,
        String icon,
        Integer sort,
        boolean visible,
        String status,
        String permissionCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 判断记录是否能形成可装载的页面路由。
     *
     * @return 是菜单且路由、组件信息齐全时返回 {@code true}
     */
    public boolean isRouteMenu() {
        return "MENU".equals(menuType)
                && routePath != null && !routePath.isBlank()
                && routeName != null && !routeName.isBlank()
                && componentKey != null && !componentKey.isBlank();
    }
}
