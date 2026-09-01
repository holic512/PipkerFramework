/**
 * @file DefaultExceptionLogReporter.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 使用 pipker.exception Logger 输出具有完整栈的 HTTP 异常日志。
 * @logic 通过请求属性对同一异常去重，提取 MDC 中的 TraceId 后保留原始 Throwable 供 SLF4J 输出。
 * @dependencies ExceptionLogReporter、PipkerLogProperties、SLF4J MDC
 * @index_tags log、exception、slf4j
 * @author holic512
 */
package com.pipker.starter.log.exception;

import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.context.LogMdcKeys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class DefaultExceptionLogReporter implements ExceptionLogReporter {

    public static final String REPORTED_ATTRIBUTE = DefaultExceptionLogReporter.class.getName() + ".reported";

    private static final Logger LOGGER = LoggerFactory.getLogger("pipker.exception");

    private final PipkerLogProperties properties;

    public DefaultExceptionLogReporter(PipkerLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public void report(HttpServletRequest request, Throwable exception) {
        if (request == null || exception == null || Boolean.TRUE.equals(request.getAttribute(REPORTED_ATTRIBUTE))) {
            return;
        }
        request.setAttribute(REPORTED_ATTRIBUTE, Boolean.TRUE);
        try {
            LOGGER.error(
                    "[EXCEPTION] method={} uri={} traceId={} requestId={} type={} message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    MDC.get(LogMdcKeys.TRACE_ID),
                    MDC.get(LogMdcKeys.REQUEST_ID),
                    exception.getClass().getName(),
                    truncate(exception.getMessage()),
                    exception
            );
        } catch (RuntimeException ignored) {
            // 异常日志本身不能影响原始异常链。
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ');
        int maxLength = Math.max(0, properties.getException().getMaxMessageLength());
        return maxLength == 0 || normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "…";
    }
}
