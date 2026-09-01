/**
 * @file LogContextContributor.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义认证、租户等模块补充请求 MDC 信息的低耦合扩展点。
 * @logic 请求 Filter 依次调用所有实现；贡献失败会被隔离且不会中断请求。
 * @dependencies LogContext
 * @index_tags log、mdc、spi
 * @author holic512
 */
package com.pipker.starter.log.context;

/**
 * 为当前请求贡献认证、租户等附加日志上下文的扩展点。
 */
@FunctionalInterface
public interface LogContextContributor {

    /**
     * 向请求日志上下文追加自定义字段。
     *
     * @param context 当前请求的受控日志上下文
     */
    void contribute(LogContext context);
}
