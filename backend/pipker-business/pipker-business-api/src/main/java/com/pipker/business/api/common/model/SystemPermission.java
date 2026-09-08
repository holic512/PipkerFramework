/**
 * @file SystemPermission.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_permission 中受保护 API 的权限定义。
 * @logic 保存权限标识、类型、状态和审计信息，不混入页面或按钮权限投影。
 * @dependencies MyBatis-Plus、Lombok、Java 标准库
 * @index_tags system-permission、rbac、api、persistence
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
