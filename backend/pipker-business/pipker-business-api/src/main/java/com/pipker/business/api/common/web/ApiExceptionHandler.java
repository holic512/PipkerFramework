/**
 * @file ApiExceptionHandler.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Maps controller and request-validation failures to the code/data/message response envelope and audits login requests rejected before authentication.
 * @logic Registered controller APIs keep HTTP 200 for business failures; malformed POST /api/auth/login requests publish one validation-failure audit event without inspecting passwords.
 * @dependencies ApiResponse、ApiBusinessException、AuthenticationAuditRecorder、Spring Web MVC
 * @index_tags api、exception、response、authentication、audit、validation
 * @author holic512
 */
package com.pipker.business.api.common.web;

import com.pipker.business.common.api.ApiResponse;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.EventType;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.FailureReason;
import com.pipker.business.common.auth.audit.AuthenticationAuditRecorder;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.business.api.system.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 统一 API 异常响应处理器。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String LOGIN_PATH = "/api/auth/login";

    private final AuthenticationAuditRecorder authenticationAuditRecorder;

    /** 创建不启用认证审计的异常处理器，供独立 MVC 测试使用。 */
    public ApiExceptionHandler() {
        this(AuthenticationAuditRecorder.NOOP);
    }

    /** 创建带认证审计能力的异常处理器。 */
    @Autowired
    public ApiExceptionHandler(AuthenticationAuditRecorder authenticationAuditRecorder) {
        this.authenticationAuditRecorder = authenticationAuditRecorder;
    }

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
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Object target = exception.getBindingResult().getTarget();
        String username = target instanceof LoginRequest loginRequest ? loginRequest.username() : null;
        recordLoginValidationFailure(request, username);
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
            HttpRequestMethodNotSupportedException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRequestValidation(
            Exception exception,
            HttpServletRequest request
    ) {
        recordLoginValidationFailure(request, null);
        return ResponseEntity.ok(ApiResponse.failure(CommonApiCode.VALIDATION_FAILED));
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

    private void recordLoginValidationFailure(HttpServletRequest request, String username) {
        if (request == null || !"POST".equalsIgnoreCase(request.getMethod())
                || !LOGIN_PATH.equals(request.getRequestURI())) {
            return;
        }
        try {
            authenticationAuditRecorder.record(AuthenticationAuditEvent.failure(
                    EventType.LOGIN,
                    FailureReason.VALIDATION_FAILED,
                    null,
                    username
            ));
        } catch (RuntimeException exception) {
            LOGGER.error("Authentication audit recorder failed and was ignored: "
                    + "eventType=LOGIN result=FAILURE failureType={}",
                    exception.getClass().getSimpleName());
        }
    }
}
