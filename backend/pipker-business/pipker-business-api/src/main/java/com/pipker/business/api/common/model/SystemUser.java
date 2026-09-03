/**
 * @file SystemUser.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 表示 system_user 中的系统登录账户记录。
 * @logic 仅承载框架通用账户字段，密码哈希不会投影到 HTTP 响应。
 * @dependencies Java 标准库
 * @index_tags system-user、rbac、persistence
 * @author holic512
 */
package com.pipker.business.api.common.model;

import java.time.LocalDateTime;

/**
 * 系统登录账户持久化模型。
 */
public record SystemUser(
        Long id,
        String username,
        String password,
        String nickname,
        String avatar,
        String phone,
        String email,
        String status,
        LocalDateTime lastLoginTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 账户是否可登录。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return "ENABLED".equals(status);
    }
}
