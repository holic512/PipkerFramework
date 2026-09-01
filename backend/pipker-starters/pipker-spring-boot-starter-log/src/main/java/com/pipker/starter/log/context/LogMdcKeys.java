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

/**
 * 日志 Starter 管理的标准 MDC 键集合。
 */
public final class LogMdcKeys {

    /**
     * 分布式链路追踪标识。
     */
    public static final String TRACE_ID = "traceId";

    /**
     * 当前 HTTP 请求标识。
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * 当前服务名称。
     */
    public static final String SERVICE_NAME = "serviceName";

    /**
     * 当前请求的客户端 IP 地址。
     */
    public static final String CLIENT_IP = "clientIp";

    /**
     * 当前 HTTP 请求方法。
     */
    public static final String HTTP_METHOD = "httpMethod";

    /**
     * 当前 HTTP 请求路径。
     */
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

    /**
     * 判断指定键是否由日志 Starter 保留管理。
     *
     * @param key 待判断的 MDC 键
     * @return 如果键属于标准保留键则返回 {@code true}
     */
    public static boolean isReserved(String key) {
        return RESERVED.contains(key);
    }
}
