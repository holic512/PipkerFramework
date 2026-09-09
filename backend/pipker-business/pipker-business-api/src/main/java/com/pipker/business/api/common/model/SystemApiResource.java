/**
 * @file SystemApiResource.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射保留在 system_api_resource 中的历史 HTTP 路径资源记录。
 * @logic 保留权限外键、HTTP 方法和 Spring MVC 路径模板的表结构映射；运行时接口授权只由 Permission 注解读取 PermissionEnum，不再使用此表匹配路径。
 * @dependencies MyBatis-Plus、Lombok
 * @index_tags system-api-resource、legacy、rbac、api、persistence
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

/** 系统 API 资源实体。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_api_resource")
public class SystemApiResource {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("permission_id")
    private Long permissionId;

    @TableField("http_method")
    private String httpMethod;

    @TableField("path_pattern")
    private String pathPattern;
}
