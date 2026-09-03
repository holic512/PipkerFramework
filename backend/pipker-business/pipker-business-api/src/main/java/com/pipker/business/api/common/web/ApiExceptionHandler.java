/**
 * @file ApiExceptionHandler.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 将 Controller、参数校验与 Sa-Token 授权失败统一映射为 code/data/message 响应。
 * @logic 所有已注册 API 保持 HTTP 200，调用方根据稳定业务编码而非 HTTP 状态分支。
 * @dependencies ApiResponse、ApiBusinessException、Sa-Token、Spring Web MVC
 * @index_tags api、exception、response
 * @author holic512
 */
package com.pipker.business.api.common.web;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 统一 API 异常响应处理器。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * 返回可预期业务失败。
     *
     * @param exception 业务异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ApiBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(ApiBusinessException exception) {
        return ResponseEntity.ok(ApiResponse.failure(exception.getCode(), exception.getMessage()));
    }

    /**
     * 返回请求参数校验失败。
     *
     * @param exception 参数校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse(CommonApiCode.VALIDATION_FAILED.getMessage());
        return ResponseEntity.ok(ApiResponse.failure(CommonApiCode.VALIDATION_FAILED, message));
    }

    /**
     * 将无法读取的 JSON、缺失参数和不支持的方法也视作稳定的请求校验失败。
     *
     * @param exception 请求格式或调用方式异常
     * @return 统一参数校验失败响应
     */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRequestValidation(Exception exception) {
        return ResponseEntity.ok(ApiResponse.failure(CommonApiCode.VALIDATION_FAILED));
    }

    /**
     * 返回 Sa-Token 权限和角色校验失败。
     *
     * @param exception Sa-Token 授权异常
     * @return 统一失败响应
     */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException exception) {
        return ResponseEntity.ok(ApiResponse.failure(CommonApiCode.AUTH_FORBIDDEN));
    }

    /**
     * 让未注册的开发路由和其他静态资源缺失维持标准 HTTP 404 语义。
     *
     * @param exception 资源未找到异常
     * @return 空的 404 响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 防止未处理异常向客户端泄露内部实现细节。
     *
     * @param exception 未预期异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleInternal(Exception exception) {
        LOGGER.error("Unhandled API exception", exception);
        return ResponseEntity.ok(ApiResponse.failure(CommonApiCode.INTERNAL_ERROR));
    }
}
