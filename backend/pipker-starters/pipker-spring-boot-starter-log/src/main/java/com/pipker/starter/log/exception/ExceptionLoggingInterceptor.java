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

/**
 * 在 Spring MVC 请求完成阶段观察并报告控制器执行异常。
 */
public class ExceptionLoggingInterceptor implements HandlerInterceptor {

    private final ExceptionLogReporter reporter;

    /**
     * 创建 MVC 异常观察器。
     *
     * @param reporter 异常报告器
     */
    public ExceptionLoggingInterceptor(ExceptionLogReporter reporter) {
        this.reporter = reporter;
    }

    /**
     * 仅在 MVC 传入异常时委托报告器处理，不改变异常解析结果。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param handler 当前处理器
     * @param exception MVC 请求处理期间发生的异常
     */
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
