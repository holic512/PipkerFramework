/**
 * @file UserManagementResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义用户管理列表、详情、可分配角色和操作确认的安全响应投影。
 * @logic 所有雪花主键以字符串返回，明确排除密码哈希与其他认证机密；用户角色同时附带状态，便于管理端识别历史停用关联。
 * @dependencies Java 标准库
 * @index_tags user、response、rbac、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.user;

import java.time.LocalDateTime;
import java.util.List;

/** 用户管理响应模型集合。 */
public final class UserManagementResponse {

    private UserManagementResponse() {
    }

    /** 用户关联角色的安全展示投影。 */
    public record UserRole(String id, String roleCode, String roleName, String status) {
    }

    /** 用户列表中的安全摘要。 */
    public record UserSummary(
            String id,
            String username,
            String nickname,
            String phone,
            String email,
            String status,
            LocalDateTime lastLoginTime,
            LocalDateTime createdAt,
            List<UserRole> roles,
            boolean protectedAccount
    ) {
    }

    /** 用户详情，补充更新时间并保留角色分配。 */
    public record UserDetail(
            String id,
            String username,
            String nickname,
            String phone,
            String email,
            String status,
            LocalDateTime lastLoginTime,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<UserRole> roles,
            boolean protectedAccount
    ) {
    }

    /** 可由用户管理页分配的启用普通角色。 */
    public record AssignableRole(String id, String roleCode, String roleName) {
    }

    /** 不泄露持久化实现的分页结果。 */
    public record PageResult<T>(long page, long pageSize, long total, List<T> records) {
    }

    /** 批量命令、删除和密码重置的安全确认结果。 */
    public record OperationResult(List<String> affectedIds) {
    }
}
