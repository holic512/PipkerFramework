/**
 * @file CommonApiCode.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 提供框架通用 HTTP API 响应的默认数值编码和消息。
 * @logic 认证、参数校验和未预期错误复用该枚举；业务模块可通过实现 ApiCode 定义自身结果。
 * @dependencies ApiCode、Java 标准库
 * @index_tags api、response、common-code
 * @author holic512
 */
package com.pipker.business.common.api;

/**
 * 框架通用的 HTTP API 结果码。
 */
public enum CommonApiCode implements ApiCode {
    SUCCESS(200, "OK"),
    VALIDATION_FAILED(400, "Request validation failed."),
    AUTH_INVALID_CREDENTIALS(401, "Username or password is incorrect."),
    AUTH_ACCOUNT_DISABLED(403, "Account is disabled."),
    AUTH_REQUIRED(401, "Authentication is required."),
    AUTH_FORBIDDEN(403, "Permission is required."),
    INTERNAL_ERROR(500, "Internal server error.");

    private final int code;
    private final String message;

    CommonApiCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
