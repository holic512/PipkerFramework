/**
 * @file SystemRolePermission.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_role_permission 中的角色 API 权限关联记录。
 * @logic 使用独立雪花主键标识关联，角色和权限字段只承担外键关系。
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

    @TableField("permission_id")
    private Long permissionId;
}
