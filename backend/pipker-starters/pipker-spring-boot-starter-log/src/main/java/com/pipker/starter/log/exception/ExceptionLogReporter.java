/**
 * @file ExceptionLogReporter.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义 MVC 异常与未来统一异常处理可复用的异常日志报告接口。
 * @logic 实现只输出诊断信息，不参与 HTTP 异常响应、状态码映射或异常吞没。
 * @dependencies Jakarta Servlet API
 * @index_tags log、exception、spi
 * @author holic512
 */
package com.pipker.starter.log.exception;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 报告一次 HTTP 请求处理过程中发生的异常。
 */
@FunctionalInterface
public interface ExceptionLogReporter {

    /**
     * 将异常交给日志或其他诊断处理器。
     *
     * @param request 发生异常的 HTTP 请求
     * @param exception 待报告的异常
     */
    void report(HttpServletRequest request, Throwable exception);
}
