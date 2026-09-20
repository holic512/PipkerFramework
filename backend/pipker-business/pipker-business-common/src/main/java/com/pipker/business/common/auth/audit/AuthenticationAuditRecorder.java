/**
 * @file AuthenticationAuditRecorder.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 定义认证生命周期审计事件的可替换记录端口。
 * @logic 认证业务和 Sa-Token 基础设施只发布安全事件，由应用层实现补充请求上下文并决定持久化方式。
 * @dependencies AuthenticationAuditEvent、AuthenticationAuditRequestContext
 * @index_tags authentication、audit、recorder、spi、login-log
 * @author holic512
 */
package com.pipker.business.common.auth.audit;

/**
 * 记录一条认证生命周期事件。
 */
@FunctionalInterface
public interface AuthenticationAuditRecorder {

    /** 无持久化能力时使用的安全空实现。 */
    AuthenticationAuditRecorder NOOP = event -> { };

    /**
     * 记录事件；应用实现必须隔离持久化异常，不能改变认证业务语义。
     *
     * @param event 安全认证事件
     */
    void record(AuthenticationAuditEvent event);

    /**
     * 在认证 Filter 尚未进入 MVC、无法从 RequestContextHolder 取请求时显式记录安全请求上下文。
     * 默认实现保持已有记录器兼容，数据库实现会覆盖此方法。
     *
     * @param event 安全认证事件
     * @param requestContext 不含认证机密的请求上下文
     */
    default void record(AuthenticationAuditEvent event, AuthenticationAuditRequestContext requestContext) {
        record(event);
    }
}
