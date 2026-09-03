/**
 * @file SystemMenuNode.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义返回给前端的树形系统菜单节点。
 * @logic 服务层按 parentId 和 sort 构造 children，前端不再维护静态后台菜单。
 * @dependencies Java 标准库
 * @index_tags system-menu、route、tree
 * @author holic512
 */
package com.pipker.business.api.system.model;

import java.util.List;

/**
 * 系统菜单树节点。
 */
public record SystemMenuNode(
        Long id,
        Long parentId,
        String name,
        String type,
        String path,
        String routeName,
        String componentKey,
        String icon,
        Integer sort,
        String permission,
        List<SystemMenuNode> children
) {
}
