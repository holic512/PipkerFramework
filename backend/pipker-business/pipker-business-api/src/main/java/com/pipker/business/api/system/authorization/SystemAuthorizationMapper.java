/**
 * @file SystemAuthorizationMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Queries system-user roles, API permissions, role-owned page menus, and API resource definitions.
 * @logic Resolves page routes only through system_role_menu while role permissions remain exclusive to protected backend API access.
 * @dependencies MyBatis, SystemMenu, SystemRole, SystemApiResource, Java Standard Library
 * @index_tags mybatis, rbac, role-menu, api-resource
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
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

    /** 查询用户启用的角色编码。 */
    @Select("""
            SELECT r.role_code
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            ORDER BY r.sort, r.role_code
            """)
    List<String> findRoleCodesByUserId(@Param("userId") long userId);

    /** 查询普通用户由角色授予的启用 API 权限。 */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            JOIN system_role_permission rp ON rp.role_id = r.id
            JOIN system_permission p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ENABLED'
              AND p.status = 'ENABLED'
              AND p.permission_type = 'API'
            ORDER BY p.permission_code
            """)
    List<String> findPermissionCodesByUserId(@Param("userId") long userId);

    /** 查询全部启用 API 权限，供 SUPER_ADMIN 自动授予。 */
    @Select("""
            SELECT permission_code
            FROM system_permission
            WHERE status = 'ENABLED' AND permission_type = 'API'
            ORDER BY permission_code
            """)
    List<String> findAllEnabledPermissionCodes();

    /** 查询全部启用且可见的目录和页面菜单。 */
    @Select("""
            SELECT id, parent_id, menu_name, menu_type, route_path, route_name,
                   component_key, icon, sort, visible, status, created_at, updated_at
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
            @Arg(column = "created_at", javaType = LocalDateTime.class),
            @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    List<SystemMenu> findAllVisibleMenus();

    /** 查询普通用户由启用角色直接关联的启用可见页面菜单。 */
    @Select("""
            SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.route_path,
                   m.route_name, m.component_key, m.icon, m.sort, m.visible, m.status,
                   m.created_at, m.updated_at
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            JOIN system_role_menu rm ON rm.role_id = r.id
            JOIN system_menu m ON m.id = rm.menu_id
            WHERE ur.user_id = #{userId}
              AND r.status = 'ENABLED'
              AND m.status = 'ENABLED'
              AND m.visible = TRUE
              AND m.menu_type = 'MENU'
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
            @Arg(column = "created_at", javaType = LocalDateTime.class),
            @Arg(column = "updated_at", javaType = LocalDateTime.class)
    })
    List<SystemMenu> findVisiblePageMenusByUserId(@Param("userId") long userId);

    /** 查询角色路由页面可展示的全部启用角色。 */
    @Select("""
            SELECT id, role_code, role_name, status, sort
            FROM system_role
            WHERE status = 'ENABLED'
            ORDER BY sort, role_code
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "role_code", javaType = String.class),
            @Arg(column = "role_name", javaType = String.class),
            @Arg(column = "status", javaType = String.class),
            @Arg(column = "sort", javaType = Integer.class)
    })
    List<SystemRole> findAllEnabledRoles();

    /** 查询一个角色，用于保存前的状态和 SUPER_ADMIN 校验。 */
    @Select("""
            SELECT id, role_code, role_name, status, sort
            FROM system_role
            WHERE id = #{roleId}
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "role_code", javaType = String.class),
            @Arg(column = "role_name", javaType = String.class),
            @Arg(column = "status", javaType = String.class),
            @Arg(column = "sort", javaType = Integer.class)
    })
    SystemRole findRoleById(@Param("roleId") long roleId);

    /** 查询配置页面内全部普通角色的已保存页面菜单关联。 */
    @Select("""
            SELECT rm.role_id, rm.menu_id
            FROM system_role_menu rm
            JOIN system_role r ON r.id = rm.role_id
            JOIN system_menu m ON m.id = rm.menu_id
            WHERE r.status = 'ENABLED'
              AND r.role_code <> 'SUPER_ADMIN'
              AND m.status = 'ENABLED'
              AND m.visible = TRUE
              AND m.menu_type = 'MENU'
            ORDER BY rm.role_id, rm.menu_id
            """)
    @ConstructorArgs({
            @Arg(column = "role_id", javaType = Long.class),
            @Arg(column = "menu_id", javaType = Long.class)
    })
    List<SystemRoleMenuAssignment> findAllEnabledRoleMenuAssignments();

    /** 校验提交的 ID 是否全部指向启用、可见的页面菜单。 */
    @Select("""
            <script>
            SELECT id
            FROM system_menu
            WHERE status = 'ENABLED'
              AND visible = TRUE
              AND menu_type = 'MENU'
              AND id IN
              <foreach collection='menuIds' item='menuId' open='(' separator=',' close=')'>
                #{menuId}
              </foreach>
            </script>
            """)
    List<Long> findEnabledVisiblePageMenuIds(@Param("menuIds") List<Long> menuIds);

    /** 清空一个普通角色的旧页面菜单关联。 */
    @Delete("DELETE FROM system_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenuAssignments(@Param("roleId") long roleId);

    /** 批量写入一个角色的新页面菜单关联。 */
    @Insert("""
            <script>
            INSERT INTO system_role_menu (role_id, menu_id) VALUES
            <foreach collection='menuIds' item='menuId' separator=','>
              (#{roleId}, #{menuId})
            </foreach>
            </script>
            """)
    void insertRoleMenuAssignments(
            @Param("roleId") long roleId,
            @Param("menuIds") List<Long> menuIds
    );

    /** 查询当前启用 API 权限所声明的受保护 HTTP 资源。 */
    @Select("""
            SELECT r.id, p.permission_code, r.http_method, r.path_pattern
            FROM system_api_resource r
            JOIN system_permission p ON p.id = r.permission_id
            WHERE p.status = 'ENABLED' AND p.permission_type = 'API'
            ORDER BY r.http_method, r.id
            """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class, id = true),
            @Arg(column = "permission_code", javaType = String.class),
            @Arg(column = "http_method", javaType = String.class),
            @Arg(column = "path_pattern", javaType = String.class)
    })
    List<SystemApiResource> findAllEnabledApiResources();
}
