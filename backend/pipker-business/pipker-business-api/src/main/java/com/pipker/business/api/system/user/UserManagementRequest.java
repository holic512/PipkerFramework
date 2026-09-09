/**
 * @file UserManagementRequest.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义用户管理创建、资料更新、密码重置与批量操作的请求契约。
 * @logic 在 HTTP 边界限制用户标识、资料字段、状态和角色 ID 的基本格式；服务层继续校验保护账户、角色可分配性与跨表关系。
 * @dependencies Jakarta Validation、Java 标准库
 * @index_tags user、request-validation、rbac、password-reset、administration
 * @author holic512
 */
package com.pipker.business.api.system.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 用户管理请求模型集合。 */
public final class UserManagementRequest {

    private UserManagementRequest() {
    }

    /** 创建账户与初始角色分配。 */
    public record Create(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "username must contain letters, digits, or underscores only")
            String username,
            @NotBlank @Size(min = 8, max = 128) String password,
            @Size(max = 100) String nickname,
            @Size(max = 32) String phone,
            @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
            String status,
            @NotEmpty List<@NotBlank String> roleIds
    ) {
    }

    /** 更新账户可变资料与完整角色分配；用户名创建后保持不变。 */
    public record Update(
            @Size(max = 100) String nickname,
            @Size(max = 32) String phone,
            @Email @Size(max = 255) String email,
            @NotBlank @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
            String status,
            @NotEmpty List<@NotBlank String> roleIds
    ) {
    }

    /** 重置用户登录密码。 */
    public record ResetPassword(@NotBlank @Size(min = 8, max = 128) String newPassword) {
    }

    /** 批量变更普通用户状态。 */
    public record BatchStatus(
            @NotEmpty List<@NotBlank String> userIds,
            @NotBlank @Pattern(regexp = "^(ENABLED|DISABLED)$", message = "status must be ENABLED or DISABLED")
            String status
    ) {
    }

    /** 批量删除普通用户。 */
    public record BatchDelete(@NotEmpty List<@NotBlank String> userIds) {
    }
}
