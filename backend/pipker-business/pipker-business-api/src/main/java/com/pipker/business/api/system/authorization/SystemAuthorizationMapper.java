/**
 * @file SystemAuthorizationMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 查询 system_user、角色、权限和菜单关联形成的实时 RBAC 数据。
 * @logic 普通用户走角色关联，SUPER_ADMIN 的全量权限和菜单由服务层集中分支处理。
 * @dependencies MyBatis、SystemMenu、Java 标准库
 * @index_tags mybatis、rbac、system-menu
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统授权关联查询 Mapper。
 */
@Mapper
public interface SystemAuthorizationMapper {

    /**
     * 查询用户启用的角色编码。
     *
     * @param userId 用户主键
     * @return 去重并排序的角色编码
     */
    @Select("""
            SELECT r.role_code
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            ORDER BY r.sort, r.role_code
            """)
    List<String> findRoleCodesByUserId(@Param("userId") long userId);

    /**
     * 查询普通用户由角色关联授予的启用权限。
     *
     * @param userId 用户主键
     * @return 去重并排序的权限编码
     */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            JOIN system_role_permission rp ON rp.role_id = r.id
            JOIN system_permission p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ENABLED'
              AND p.status = 'ENABLED'
            ORDER BY p.permission_code
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") long userId);

    /**
     * 查询框架全部启用权限。
     *
     * @return 权限编码列表
     */
    @Select("""
            SELECT permission_code
            FROM system_permission
            WHERE status = 'ENABLED'
            ORDER BY permission_code
            """)
    List<String> findAllEnabledPermissionCodes();

    /**
     * 查询普通用户可见的菜单。
     *
     * @param userId 用户主键
     * @return 菜单列表
     */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.route_path,
                   m.route_name, m.component_key, m.icon, m.sort, m.visible, m.status,
                   m.permission_code, m.created_at, m.updated_at
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            JOIN system_role_menu rm ON rm.role_id = r.id
            JOIN system_menu m ON m.id = rm.menu_id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ENABLED'
              AND m.status = 'ENABLED'
              AND m.visible = TRUE
            ORDER BY m.sort, m.id
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "parent_id", javaType = Long.class),
            @Arg(column = "menu_name", javaType = String.class),
            @Arg(column = "menu_type", javaType = String.class),
            @Arg(column = "route_path", javaType = String.class),
            @Arg(column = "route_name", javaType = String.class),
            @Arg(column = "component_key", javaType = String.class),
            @Arg(column = "icon", javaType = String.class),
            @Arg(column = "sort", javaType = Integer.class),
            @Arg(column = "visible", javaType = boolean.class),
            @Arg(column = "status", javaType = String.class),
            @Arg(column = "permission_code", javaType = String.class),
            @Arg(column = "created_at", javaType = LocalDateTime.class),
            @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    List<SystemMenu> findVisibleMenusByUserId(@Param("userId") long userId);

    /**
     * 查询框架全部可见菜单。
     *
     * @return 菜单列表
     */
    @Select("""
            SELECT id, parent_id, menu_name, menu_type, route_path, route_name,
                   component_key, icon, sort, visible, status, permission_code,
                   created_at, updated_at
            FROM system_menu
            WHERE status = 'ENABLED' AND visible = TRUE
            ORDER BY sort, id
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "parent_id", javaType = Long.class),
            @Arg(column = "menu_name", javaType = String.class),
            @Arg(column = "menu_type", javaType = String.class),
            @Arg(column = "route_path", javaType = String.class),
            @Arg(column = "route_name", javaType = String.class),
            @Arg(column = "component_key", javaType = String.class),
            @Arg(column = "icon", javaType = String.class),
            @Arg(column = "sort", javaType = Integer.class),
            @Arg(column = "visible", javaType = boolean.class),
            @Arg(column = "status", javaType = String.class),
            @Arg(column = "permission_code", javaType = String.class),
            @Arg(column = "created_at", javaType = LocalDateTime.class),
            @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    List<SystemMenu> findAllVisibleMenus();
}
