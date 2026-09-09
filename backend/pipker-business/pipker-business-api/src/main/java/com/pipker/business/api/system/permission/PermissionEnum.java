/**
 * @file PermissionEnum.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义系统唯一有效的接口权限点及其面向管理端的展示元数据。
 * @logic 枚举编码同时被接口注解、角色权限保存校验和权限点列表读取复用；新增权限必须先在此声明，数据库记录只保存已选编码而不定义权限本身。
 * @dependencies Java Standard Library
 * @index_tags permission、enum、rbac、api-authorization、administration
 * @author holic512
 */
package com.pipker.business.api.system.permission;

/**
 * 系统接口权限点。
 *
 * <p>此枚举是运行时唯一有效的权限定义来源。权限编码使用连字符分隔，
 * 便于管理端按前缀进行轻量分组展示。</p>
 */
public enum PermissionEnum {

    SYSTEM_AUTHORIZATION_VIEW(
            "system-authorization-view",
            "查看当前授权",
            "读取当前登录用户的角色、接口权限和页面路由授权"
    ),
    SYSTEM_ROLE_MANAGE(
            "system-role-manage",
            "管理系统角色",
            "维护角色资料、成员、页面路由权限和接口权限"
    ),
    SYSTEM_ROUTE_VIEW(
            "system-route-view",
            "查看系统路由",
            "查看已落库页面路径、路由索引、菜单展示状态和目录分类"
    ),
    SYSTEM_ROUTE_MANAGE(
            "system-route-manage",
            "管理系统路由配置",
            "维护菜单名称、图标、排序、展示和启用状态，不改变路由结构"
    ),
    SYSTEM_USER_VIEW(
            "system-user-view",
            "查看系统用户",
            "预留给系统用户查询与详情读取操作"
    ),
    SYSTEM_USER_MANAGE(
            "system-user-manage",
            "管理系统用户",
            "预留给系统用户创建、编辑、状态维护和密码重置操作"
    );

    private final String code;
    private final String name;
    private final String description;

    PermissionEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /** @return 保存到角色权限关联表的稳定权限编码。 */
    public String getCode() {
        return code;
    }

    /** @return 管理端展示名称。 */
    public String getName() {
        return name;
    }

    /** @return 管理端展示的权限用途说明。 */
    public String getDescription() {
        return description;
    }
}
