/**
 * @file DatabaseAuthenticationAuditRecorder.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 将统一认证审计事件补充当前 HTTP 与 Trace 上下文后安全写入 system_login_log。
 * @logic 对外部文本按列长度截断，委托独立事务 Writer 插入；任何持久化异常只输出脱敏告警，不改变认证调用结果。
 * @dependencies AuthenticationAuditRecorder、SystemLoginLogWriter、Servlet API、SLF4J MDC
 * @index_tags authentication、audit、login-log、trace、non-blocking
 * @author holic512
 */
package com.pipker.business.api.system.loginlog;

import com.pipker.business.api.common.model.SystemLoginLog;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent;
import com.pipker.business.common.auth.audit.AuthenticationAuditRecorder;
import com.pipker.business.common.auth.audit.AuthenticationAuditRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** 数据库认证审计记录器。 */
@Component
public class DatabaseAuthenticationAuditRecorder implements AuthenticationAuditRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAuthenticationAuditRecorder.class);
    private static final String TRACE_ID_MDC_KEY = "traceId";

    private final SystemLoginLogWriter writer;

    /** 创建数据库记录器。 */
    public DatabaseAuthenticationAuditRecorder(SystemLoginLogWriter writer) {
        this.writer = writer;
    }

    /** 补充请求元数据并以尽力而为方式写入数据库。 */
    @Override
    public void record(AuthenticationAuditEvent event) {
        record(event, requestContext(currentRequest()));
    }

    /** 使用显式 Filter 上下文或当前 MVC 请求补充审计元数据。 */
    @Override
    public void record(AuthenticationAuditEvent event, AuthenticationAuditRequestContext requestContext) {
        if (event == null) {
            return;
        }
        SystemLoginLog log = toLog(event, requestContext);
        try {
            writer.write(log);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Authentication audit persistence failed: eventType={} result={} path={} traceId={} failureType={}",
                    log.getEventType(),
                    log.getResult(),
                    log.getRequestPath(),
                    log.getTraceId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private SystemLoginLog toLog(AuthenticationAuditEvent event, AuthenticationAuditRequestContext requestContext) {
        SystemLoginLog log = new SystemLoginLog();
        log.setUserId(event.userId());
        log.setUsername(truncate(normalize(event.username()), 255));
        log.setEventType(event.eventType().name());
        log.setResult(event.result().name());
        log.setFailureReason(event.failureReason() == null ? null : event.failureReason().name());
        if (requestContext != null) {
            log.setClientIp(truncate(normalize(requestContext.clientIp()), 64));
            log.setUserAgent(truncate(normalize(requestContext.userAgent()), 512));
            log.setHttpMethod(truncate(normalize(requestContext.httpMethod()), 16));
            log.setRequestPath(truncate(normalize(requestContext.requestPath()), 255));
        }
        log.setTraceId(truncate(normalize(MDC.get(TRACE_ID_MDC_KEY)), 128));
        log.setOccurredAt(LocalDateTime.now());
        return log;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private AuthenticationAuditRequestContext requestContext(HttpServletRequest request) {
        return request == null ? null : new AuthenticationAuditRequestContext(
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getMethod(),
                request.getRequestURI()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
