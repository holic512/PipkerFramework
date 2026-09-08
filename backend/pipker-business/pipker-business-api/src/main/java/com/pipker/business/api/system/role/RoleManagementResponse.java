/**
 * @file RoleManagementResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义角色列表、详情、成员和批量操作的安全响应投影。
 * @logic 对外将雪花主键序列化为字符串，并有意排除成员账户的密码、邮箱、手机号等私密字段。
 * @dependencies Java 标准库
 * @index_tags rbac、role、response、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.role;

import java.time.LocalDateTime;
import java.util.List;

/** 角色管理响应模型集合。 */
public final class RoleManagementResponse {

    private RoleManagementResponse() {
    }

    /** 角色列表的安全摘要。 */
    public record RoleSummary(
            String id,
            String roleCode,
            String roleName,
            String description,
            String status,
            Integer sort,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /** 含成员数量的角色详情。 */
    public record RoleDetail(
            String id,
            String roleCode,
            String roleName,
            String description,
            String status,
            Integer sort,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long memberCount
    ) {
    }

    /** 可由角色管理员查看的成员账户资料。 */
    public record RoleMember(
            String id,
            String username,
            String nickname,
            String status,
            LocalDateTime lastLoginTime,
            LocalDateTime createdAt
    ) {
    }

    /** 不泄露 MyBatis-Plus 内部字段的通用分页结果。 */
    public record PageResult<T>(long page, long pageSize, long total, List<T> records) {
    }

    /** 批量或密码重置命令的安全确认结果。 */
    public record OperationResult(List<String> affectedIds) {
    }
}
