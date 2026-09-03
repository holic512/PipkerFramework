/**
 * @file SystemAuthorizationSnapshot.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 汇总当前系统用户实时查询得到的角色、权限和菜单。
 * @logic Controller 和 Sa-Token 授权适配器共享同一服务结果，确保 RBAC 数据来源唯一。
 * @dependencies SystemUserProfile、SystemMenuNode、Java 标准库
 * @index_tags rbac、authorization、api
 * @author holic512
 */
package com.pipker.business.api.common.model;

import java.util.List;

/**
 * 用户授权信息快照。
 */
public record SystemAuthorizationSnapshot(
        SystemUserProfile user,
        List<String> roles,
        List<String> permissions,
        List<SystemMenuNode> menus
) {
}
