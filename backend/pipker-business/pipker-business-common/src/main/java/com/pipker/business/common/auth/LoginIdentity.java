/**
 * 文件：LoginIdentity.java
 * 项目：Pipker Framework
 * 模块：Pipker Business Common
 * 说明：表示创建和解析已认证应用会话所需的最小公开身份信息。
 * 处理逻辑：将登录域与域内用户标识组合保存，同时不依赖账户持久化、角色或 Sa-Token API。
 * 依赖：LoginType、Java 标准库
 * 检索关键词：business、common、认证、身份、登录
 * 作者：holic512
 */
package com.pipker.business.common.auth;

import java.util.Objects;

public record LoginIdentity(LoginType loginType, String userId) {

    public LoginIdentity {
        Objects.requireNonNull(loginType, "loginType must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        userId = userId.trim();
        if (userId.isEmpty() || userId.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
