/**
 * @file ApiAuthorizationDeniedException.java
 * @project Pipker Framework
 * @module Pipker Sa-Token Starter
 * @description Marks a protected request that the configured database authorization service explicitly rejected.
 * @logic Lets the Sa-Token filter distinguish a deliberate authorization denial from a login failure or authorization infrastructure error.
 * @dependencies Java Standard Library
 * @index_tags starter, authorization, exception
 * @author holic512
 */
package com.pipker.starter.satoken.service;

/**
 * 数据库授权明确拒绝请求时使用的异常。
 */
public class ApiAuthorizationDeniedException extends RuntimeException {

    /**
     * 创建拒绝异常。
     *
     * @param message 安全的内部拒绝原因
     */
    public ApiAuthorizationDeniedException(String message) {
        super(message);
    }

    /**
     * 创建带根因的拒绝异常。
     *
     * @param message 安全的内部拒绝原因
     * @param cause 根因
     */
    public ApiAuthorizationDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
