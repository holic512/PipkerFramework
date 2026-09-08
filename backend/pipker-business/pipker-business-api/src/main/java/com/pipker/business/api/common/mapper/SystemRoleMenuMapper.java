/**
 * @file SystemRoleMenuMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_role_menu 的 MyBatis-Plus CRUD 与角色菜单配置查询。
 * @logic 关联记录使用独立雪花主键；配置读取时仅返回可配置角色与页面菜单的有效关联。
 * @dependencies MyBatis-Plus、SystemRoleMenu
 * @index_tags mybatis-plus、system-role-menu、rbac、role-menu
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
              AND m.visible = TRUE
              AND m.menu_type = 'MENU'
            ORDER BY rm.role_id, rm.menu_id
            """)
    List<SystemRoleMenu> findAllEnabledPageMenuAssignments();
}
