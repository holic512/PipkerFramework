/**
 * @file SystemUserRole.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_user_role 中的用户角色关联记录。
 * @logic 使用独立雪花主键标识关联，用户和角色字段只承担外键关系。
 * @dependencies MyBatis-Plus、Lombok
 * @index_tags system-user-role、rbac、association、persistence
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

/** 用户角色关联实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_user_role")
public class SystemUserRole {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;
}
