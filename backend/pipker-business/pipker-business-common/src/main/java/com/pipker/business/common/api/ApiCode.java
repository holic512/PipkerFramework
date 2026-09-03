/**
 * @file ApiCode.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 定义可由任意业务结果枚举实现的 HTTP API 结果码契约。
 * @logic ApiResponse 仅依赖该契约读取数值编码和默认消息，业务模块可自行维护领域结果枚举。
 * @dependencies Java 标准库
 * @index_tags api、response、error-code
 * @author holic512
 */
package com.pipker.business.common.api;

/**
 * Pipker HTTP API 结果码契约。
 */
public interface ApiCode {

    /**
     * 获取接口响应中使用的数值状态码。
     *
     * @return 数值状态码
     */
    int getCode();

    /**
     * 获取接口响应默认返回的消息。
     *
     * @return 默认消息
     */
    String getMessage();
}
