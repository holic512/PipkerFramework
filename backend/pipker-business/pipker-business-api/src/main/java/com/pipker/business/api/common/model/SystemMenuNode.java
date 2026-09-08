/**
 * @file SystemMenuNode.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义返回给前端的树形系统菜单节点。
 * @logic 服务层按 parentId 和 sort 构造 children，将雪花主键序列化为字符串，并携带菜单展示状态供导航与角色路由配置分别消费。
 * @dependencies Java 标准库
 * @index_tags system-menu、route、tree、snowflake-id
 * @author holic512
 */
package com.pipker.business.api.common.model;

import java.util.List;

/**
 * 系统菜单树节点。
 */
public record SystemMenuNode(
        String id,
        String parentId,
        String name,
        String type,
        String path,
        String routeName,
        String componentKey,
        String icon,
        Integer sort,
        boolean visible,
        List<SystemMenuNode> children
) {
}
