/**
 * @file SystemRole.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_role 中的系统角色及其生命周期字段。
 * @logic 使用 MyBatis-Plus 雪花主键和审计字段填充，供角色菜单和授权查询共用。
 * @dependencies MyBatis-Plus、Lombok、Java 标准库
 * @index_tags system-role、rbac、persistence、mybatis-plus
 * @author holic512
 */
package com.pipker.business.api.common.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 系统角色实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_role")
public class SystemRole {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("role_code")
    private String roleCode;

    @TableField("role_name")
    private String roleName;

    @TableField("description")
    private String description;

    @TableField("status")
    private String status;

    @TableField("sort")
    private Integer sort;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** @return 角色处于启用状态时返回 {@code true}。 */
    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
