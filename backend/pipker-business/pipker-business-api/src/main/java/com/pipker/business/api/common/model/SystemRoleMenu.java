/**
 * @file SystemRoleMenu.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_role_menu 中的角色页面菜单关联记录。
 * @logic 使用独立雪花主键标识关联，角色和菜单字段只承担外键关系。
 * @dependencies MyBatis-Plus、Lombok
 * @index_tags system-role-menu、rbac、association、persistence
 * @author holic512
 */
package com.pipker.business.api.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 角色菜单关联实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_role_menu")
public class SystemRoleMenu {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("role_id")
    private Long roleId;

    @TableField("menu_id")
    private Long menuId;
}
