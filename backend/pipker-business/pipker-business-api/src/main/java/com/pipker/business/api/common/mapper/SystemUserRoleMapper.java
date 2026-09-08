/**
 * @file SystemUserRoleMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_user_role 的 MyBatis-Plus CRUD 与用户授权关联查询。
 * @logic 单表关联记录走 BaseMapper；角色、API 权限和页面菜单查询在此集中保留必要连接。
 * @dependencies MyBatis-Plus、SystemUserRole、SystemMenu
 * @index_tags mybatis-plus、system-user-role、rbac、authorization
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemMenu;
import com.pipker.business.api.common.model.SystemUserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 用户角色关联 Mapper。 */
public interface SystemUserRoleMapper extends BaseMapper<SystemUserRole> {

    /** 查询用户启用的角色编码。 */
    @Select("""
            SELECT r.role_code
            FROM system_user_role ur
            JOIN system_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 'ENABLED'
            ORDER BY r.sort, r.role_code
            """)
    List<String> findEnabledRoleCodesByUserId(@Param("userId") long userId);

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
    List<String> findEnabledPermissionCodesByUserId(@Param("userId") long userId);

    /** 查询普通用户由启用角色关联的启用可见页面菜单。 */
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
    List<SystemMenu> findVisiblePageMenusByUserId(@Param("userId") long userId);
}
