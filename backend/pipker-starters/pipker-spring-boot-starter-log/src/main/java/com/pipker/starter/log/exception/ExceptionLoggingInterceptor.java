/**
 * @file ExceptionLoggingInterceptor.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 在 Spring MVC 请求完成时观察控制器执行异常并委托异常日志报告器。
 * @logic 仅在 afterCompletion 收到异常时报告，不处理、不替换也不修改 MVC 的异常解析结果。
 * @dependencies Spring Web MVC、ExceptionLogReporter
 * @index_tags log、exception、mvc
 * @author holic512
 */
package com.pipker.starter.log.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class ExceptionLoggingInterceptor implements HandlerInterceptor {

    private final ExceptionLogReporter reporter;

    public ExceptionLoggingInterceptor(ExceptionLogReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        if (exception != null) {
            reporter.report(request, exception);
        }
    }
}
