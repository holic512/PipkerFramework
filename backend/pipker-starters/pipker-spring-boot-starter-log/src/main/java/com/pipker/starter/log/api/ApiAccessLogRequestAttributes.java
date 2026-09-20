/**
 * @file ApiAccessLogRequestAttributes.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 集中声明 AOP 与请求 Filter 交换接口元数据的 Servlet 请求属性键。
 * @logic 切面写入方法、注解、耗时和异常信息，Filter 在请求最终完成时读取并清理，不依赖线程本地状态。
 * @dependencies Java Standard Library
 * @index_tags log、api、request-attributes、aop
 * @author holic512
 */
package com.pipker.starter.log.api;

/** 日志 Starter 内部共享的请求属性键。 */
public final class ApiAccessLogRequestAttributes {

    private static final String PREFIX = ApiAccessLogRequestAttributes.class.getName() + ".";
    public static final String HANDLER_METHOD = PREFIX + "handlerMethod";
    public static final String PURPOSE = PREFIX + "purpose";
    public static final String REMARK = PREFIX + "remark";
    public static final String HANDLER_DURATION_MS = PREFIX + "handlerDurationMs";
    public static final String EXCEPTION_TYPE = PREFIX + "exceptionType";
    public static final String EXCEPTION_MESSAGE = PREFIX + "exceptionMessage";
    public static final String ASYNC_FAILURE = PREFIX + "asyncFailure";

    private ApiAccessLogRequestAttributes() {
    }
}
