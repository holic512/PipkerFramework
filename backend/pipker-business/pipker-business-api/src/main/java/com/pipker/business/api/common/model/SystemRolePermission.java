/**
 * @file SystemRolePermission.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_role_permission 中的角色与接口权限编码关联记录。
 * @logic 使用独立雪花主键标识关联；role_id 保持角色外键，permission_code 原样保存角色选择过的有效或历史枚举编码。
 * @dependencies MyBatis-Plus、Lombok
 * @index_tags system-role-permission、rbac、association、persistence
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

/** 角色权限关联实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_role_permission")
public class SystemRolePermission {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("role_id")
    private Long roleId;

    @TableField("permission_code")
    private String permissionCode;
}
