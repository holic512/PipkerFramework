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

/**
 * 将 HTTP 异常以完整堆栈写入 {@code pipker.exception} Logger 的默认实现。
 */
public class DefaultExceptionLogReporter implements ExceptionLogReporter {

    /**
     * 标记请求是否已经报告过异常的 Servlet 请求属性名。
     */
    public static final String REPORTED_ATTRIBUTE = DefaultExceptionLogReporter.class.getName() + ".reported";

    private static final Logger LOGGER = LoggerFactory.getLogger("pipker.exception");

    private final PipkerLogProperties properties;

    /**
     * 使用日志配置创建默认异常报告器。
     *
     * @param properties 日志配置
     */
    public DefaultExceptionLogReporter(PipkerLogProperties properties) {
        this.properties = properties;
    }

    /**
     * 报告异常；同一请求只报告一次，且报告失败不会影响原始异常传播。
     *
     * @param request 发生异常的 HTTP 请求
     * @param exception 待报告的异常
     */
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
