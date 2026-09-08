/**
 * @file SystemRoleMenuMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_role_menu 的 MyBatis-Plus CRUD 与角色页面权限配置查询。
 * @logic 关联记录使用独立雪花主键；配置读取只保留已具备路径、名称和页面索引的启用页面关联，隐藏页面可授权但不进入导航。
 * @dependencies MyBatis-Plus、SystemRoleMenu
 * @index_tags mybatis-plus、system-role-menu、rbac、role-menu、route、hidden-menu
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemRoleMenu;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色菜单关联 Mapper。 */
public interface SystemRoleMenuMapper extends BaseMapper<SystemRoleMenu> {

    /** 查询配置页面内全部普通角色的已保存页面菜单关联。 */
    @Select("""
            SELECT rm.id, rm.role_id, rm.menu_id
            FROM system_role_menu rm
            JOIN system_role r ON r.id = rm.role_id
            JOIN system_menu m ON m.id = rm.menu_id
            WHERE r.status = 'ENABLED'
              AND r.role_code <> 'SUPER_ADMIN'
              AND m.status = 'ENABLED'
              AND m.menu_type = 'MENU'
              AND m.route_path IS NOT NULL AND TRIM(m.route_path) <> ''
              AND m.route_name IS NOT NULL AND TRIM(m.route_name) <> ''
              AND m.component_key IS NOT NULL AND TRIM(m.component_key) <> ''
            ORDER BY rm.role_id, rm.menu_id
            """)
    List<SystemRoleMenu> findAllEnabledPageMenuAssignments();

    /** 查询一个角色已保存的启用页面权限，保留隐藏导航页面。 */
    @Select("""
            SELECT rm.id, rm.role_id, rm.menu_id
            FROM system_role_menu rm
            JOIN system_menu m ON m.id = rm.menu_id
            WHERE rm.role_id = #{roleId}
              AND m.status = 'ENABLED'
              AND m.menu_type = 'MENU'
              AND m.route_path IS NOT NULL AND TRIM(m.route_path) <> ''
              AND m.route_name IS NOT NULL AND TRIM(m.route_name) <> ''
              AND m.component_key IS NOT NULL AND TRIM(m.component_key) <> ''
            ORDER BY rm.menu_id
            """)
    List<SystemRoleMenu> findEnabledPageMenuAssignmentsByRoleId(Long roleId);
}
