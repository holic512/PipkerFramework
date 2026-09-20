/**
 * @file AuthenticationAuditEvent.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 定义跨认证层传递的安全审计事件，不携带密码、令牌或请求体。
 * @logic 使用受控事件、结果和失败原因枚举表达登录、登出与当前会话鉴权结果，并允许基础设施补充请求元数据后持久化。
 * @dependencies Java 标准库
 * @index_tags authentication、audit、login-log、event、security
 * @author holic512
 */
package com.pipker.business.common.auth.audit;

import java.util.Objects;

/**
 * 一条已经去除认证机密的认证生命周期事件。
 */
public record AuthenticationAuditEvent(
        EventType eventType,
        Result result,
        FailureReason failureReason,
        Long userId,
        String username
) {

    /** 创建事件并约束成功、失败原因之间的一致性。 */
    public AuthenticationAuditEvent {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(result, "result");
        if (result == Result.SUCCESS && failureReason != null) {
            throw new IllegalArgumentException("Successful authentication audit events cannot have a failure reason");
        }
        if (result == Result.FAILURE && failureReason == null) {
            throw new IllegalArgumentException("Failed authentication audit events require a failure reason");
        }
    }

    /** 创建成功事件。 */
    public static AuthenticationAuditEvent success(EventType eventType, Long userId, String username) {
        return new AuthenticationAuditEvent(eventType, Result.SUCCESS, null, userId, username);
    }

    /** 创建失败事件。 */
    public static AuthenticationAuditEvent failure(
            EventType eventType,
            FailureReason failureReason,
            Long userId,
            String username
    ) {
        return new AuthenticationAuditEvent(eventType, Result.FAILURE, failureReason, userId, username);
    }

    /** 认证生命周期事件类型。 */
    public enum EventType {
        LOGIN,
        LOGOUT,
        AUTH_CHECK
    }

    /** 认证事件结果。 */
    public enum Result {
        SUCCESS,
        FAILURE
    }

    /** 对外稳定、不会泄露底层异常细节的失败原因。 */
    public enum FailureReason {
        INVALID_CREDENTIALS,
        ACCOUNT_DISABLED,
        AUTH_REQUIRED,
        AUTH_FORBIDDEN,
        VALIDATION_FAILED,
        INTERNAL_ERROR
    }
}
