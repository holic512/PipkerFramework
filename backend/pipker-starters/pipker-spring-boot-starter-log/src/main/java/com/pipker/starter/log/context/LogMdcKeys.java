/**
 * @file LogMdcKeys.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 集中声明由日志 Starter 管理的 MDC 键。
 * @logic 这些键由请求 Filter 建立和恢复，扩展贡献者不能覆盖其值。
 * @dependencies Java 标准库
 * @index_tags log、mdc、trace
 * @author holic512
 */
package com.pipker.starter.log.context;

import java.util.Set;

public final class LogMdcKeys {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";
    public static final String SERVICE_NAME = "serviceName";
    public static final String CLIENT_IP = "clientIp";
    public static final String HTTP_METHOD = "httpMethod";
    public static final String REQUEST_URI = "requestUri";

    private static final Set<String> RESERVED = Set.of(
            TRACE_ID,
            REQUEST_ID,
            SERVICE_NAME,
            CLIENT_IP,
            HTTP_METHOD,
            REQUEST_URI
    );

    private LogMdcKeys() {
    }

    public static boolean isReserved(String key) {
        return RESERVED.contains(key);
    }
}
