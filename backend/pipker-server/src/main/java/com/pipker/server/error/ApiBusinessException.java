/**
 * @file ApiBusinessException.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 表示能够安全映射为统一 API 响应的预期业务失败。
 * @logic 认证编排和当前会话解析使用 ApiCode 抛出该异常，默认消息由结果枚举统一维护，避免 Controller 依赖数据库异常细节。
 * @dependencies ApiCode
 * @index_tags api、error、authentication
 * @author holic512
 */
package com.pipker.server.error;

import com.pipker.business.common.api.ApiCode;

/**
 * 可公开的业务失败。
 */
public class ApiBusinessException extends RuntimeException {

    private final ApiCode code;

    /**
     * 创建使用结果码默认消息的业务失败。
     *
     * @param code 稳定错误码
     */
    public ApiBusinessException(ApiCode code) {
        this(code, code.getMessage());
    }

    /**
     * 创建业务失败。
     *
     * @param code 稳定错误码
     * @param message 覆盖枚举默认值的安全错误消息
     */
    public ApiBusinessException(ApiCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 返回稳定错误码。
     *
     * @return 错误码
     */
    public ApiCode getCode() {
        return code;
    }
}
