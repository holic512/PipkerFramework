/**
 * @file SystemPermission.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射保留在 system_permission 中的历史 API 权限定义记录。
 * @logic 保留原有权限标识、类型、状态和审计字段以兼容既有系统表；运行时有效接口权限只由 PermissionEnum 定义，不从此表读取。
 * @dependencies MyBatis-Plus、Lombok、Java 标准库
 * @index_tags system-permission、legacy、rbac、api、persistence
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

/** 系统权限实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_permission")
public class SystemPermission {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("permission_code")
    private String permissionCode;

    @TableField("permission_name")
    private String permissionName;

    @TableField("permission_type")
    private String permissionType;

    @TableField("description")
    private String description;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
