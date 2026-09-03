/**
 * @file ApiResponse.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 定义全部已注册 HTTP API 共用的 code/data/message 响应契约。
 * @logic 成功和失败均从 ApiCode 读取稳定数值编码与默认消息，传输层调用方统一读取数据与消息。
 * @dependencies ApiCode、CommonApiCode、Java 标准库
 * @index_tags api、response、contract
 * @author holic512
 */
package com.pipker.business.common.api;

/**
 * 统一 HTTP API 响应。
 *
 * @param code 稳定的数值业务状态码
 * @param data 成功结果或可选失败上下文
 * @param message 面向调用方的结果说明
 * @param <T> 数据类型
 */
public record ApiResponse<T>(
        int code,
        T data,
        String message
) {

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(CommonApiCode.SUCCESS, data);
    }

    /**
     * 创建携带自定义成功结果码的响应。
     *
     * @param code 结果码枚举
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(ApiCode code, T data) {
        return new ApiResponse<>(code.getCode(), data, code.getMessage());
    }

    /**
     * 创建使用结果码默认消息的失败响应。
     *
     * @param code 失败结果码枚举
     * @return 不携带数据的失败响应
     */
    public static ApiResponse<Void> failure(ApiCode code) {
        return failure(code, code.getMessage());
    }

    /**
     * 创建失败响应。
     *
     * @param code 失败结果码枚举
     * @param message 覆盖枚举默认值的失败说明
     * @return 不携带数据的失败响应
     */
    public static ApiResponse<Void> failure(ApiCode code, String message) {
        return new ApiResponse<>(code.getCode(), null, message);
    }
}
