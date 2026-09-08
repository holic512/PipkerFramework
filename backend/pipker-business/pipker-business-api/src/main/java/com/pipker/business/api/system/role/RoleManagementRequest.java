/**
 * @file RoleManagementRequest.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义角色管理 API 的创建、编辑、批量、页面权限与成员密码重置请求契约。
 * @logic 在 HTTP 边界校验字段长度和基础格式，服务层继续处理角色保护规则、页面路由有效性和跨表完整性校验。
 * @dependencies Jakarta Validation、Java 标准库
 * @index_tags rbac、role、route、request-validation、administration
 * @author holic512
 */
package com.pipker.business.api.system.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 角色管理请求模型集合。 */
public final class RoleManagementRequest {

    private RoleManagementRequest() {
    }

    /** 创建角色。 */
    public record Create(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "roleCode must contain letters, digits, or underscores only")
            String roleCode,
            @NotBlank @Size(max = 100) String roleName,
            @Size(max = 500) String description,
            @NotBlank @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
            String status,
            @NotNull Integer sort
    ) {
    }

    /** 编辑角色的可变资料；角色编码创建后保持不变。 */
    public record Update(
            @NotBlank @Size(max = 100) String roleName,
            @Size(max = 500) String description,
            @NotBlank @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
            String status,
            @NotNull Integer sort
    ) {
    }

    /** 批量变更角色状态。 */
    public record BatchStatus(
            @NotEmpty List<@NotBlank String> roleIds,
            @NotBlank @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
            String status
    ) {
    }

    /** 批量删除角色。 */
    public record BatchDelete(@NotEmpty List<@NotBlank String> roleIds) {
    }

    /** 全量替换一个角色可访问的页面路由；空列表代表移除全部页面权限。 */
    public record ReplaceRoutes(
            @NotNull List<@NotBlank String> routeIds
    ) {
    }

    /** 重置角色成员账户的登录密码。 */
    public record ResetMemberPassword(@NotBlank @Size(min = 8, max = 128) String newPassword) {
    }
}
