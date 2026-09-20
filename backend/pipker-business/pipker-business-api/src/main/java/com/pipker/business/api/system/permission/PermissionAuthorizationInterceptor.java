/**
 * @file PermissionAuthorizationInterceptor.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 在 MVC 请求进入标注接口前按当前用户的有效权限点执行统一授权校验，并审计 /api/auth/me 的鉴权结果。
 * @logic 仅处理 HandlerMethod，优先读取方法注解后回退到控制器类注解；只为当前会话鉴权接口发布一次成功或失败审计事件，其他 API 不产生登录日志。
 * @dependencies Permission、CurrentSystemAuthorizationService、AuthenticationAuditRecorder、ApiBusinessException、Spring Web MVC
 * @index_tags permission、interceptor、rbac、api-authorization、spring-mvc、authentication、audit
 * @author holic512
 */
package com.pipker.business.api.system.permission;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.EventType;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.FailureReason;
import com.pipker.business.common.auth.audit.AuthenticationAuditRecorder;
import com.pipker.business.common.exception.ApiBusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 基于 {@link Permission} 的接口权限拦截器。
 */
@Component
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionAuthorizationInterceptor.class);
    private static final String CURRENT_AUTHORIZATION_PATH = "/api/auth/me";

    private final CurrentSystemAuthorizationService currentSystemAuthorizationService;
    private final AuthenticationAuditRecorder authenticationAuditRecorder;

    /** 创建不启用审计的接口权限拦截器，供独立单元测试使用。 */
    public PermissionAuthorizationInterceptor(CurrentSystemAuthorizationService currentSystemAuthorizationService) {
        this(currentSystemAuthorizationService, AuthenticationAuditRecorder.NOOP);
    }

    /** 创建接口权限拦截器。 */
    @Autowired
    public PermissionAuthorizationInterceptor(
            CurrentSystemAuthorizationService currentSystemAuthorizationService,
            AuthenticationAuditRecorder authenticationAuditRecorder
    ) {
        this.currentSystemAuthorizationService = currentSystemAuthorizationService;
        this.authenticationAuditRecorder = authenticationAuditRecorder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Permission permission = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(),
                Permission.class
        );
        if (permission == null) {
            permission = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(),
                    Permission.class
            );
        }
        if (permission == null) {
            return true;
        }

        SystemAuthorizationSnapshot snapshot;
        try {
            snapshot = currentSystemAuthorizationService.currentSnapshot();
        } catch (ApiBusinessException exception) {
            recordAuthCheckFailure(request, failureReason(exception), null);
            throw exception;
        } catch (RuntimeException exception) {
            recordAuthCheckFailure(request, FailureReason.INTERNAL_ERROR, null);
            throw exception;
        }

        boolean granted = snapshot.permissions().contains(permission.value().getCode());
        if (!granted) {
            recordAuthCheckFailure(request, FailureReason.AUTH_FORBIDDEN, snapshot);
            throw new ApiBusinessException(CommonApiCode.AUTH_FORBIDDEN);
        }
        if (isCurrentAuthorizationRequest(request)) {
            recordAudit(AuthenticationAuditEvent.success(
                    EventType.AUTH_CHECK,
                    snapshot.user() == null ? null : snapshot.user().id(),
                    snapshot.user() == null ? null : snapshot.user().username()
            ));
        }
        return true;
    }

    private void recordAuthCheckFailure(
            HttpServletRequest request,
            FailureReason failureReason,
            SystemAuthorizationSnapshot snapshot
    ) {
        if (!isCurrentAuthorizationRequest(request)) {
            return;
        }
        recordAudit(AuthenticationAuditEvent.failure(
                EventType.AUTH_CHECK,
                failureReason,
                snapshot == null || snapshot.user() == null ? null : snapshot.user().id(),
                snapshot == null || snapshot.user() == null ? null : snapshot.user().username()
        ));
    }

    private boolean isCurrentAuthorizationRequest(HttpServletRequest request) {
        return request != null && CURRENT_AUTHORIZATION_PATH.equals(request.getRequestURI());
    }

    private FailureReason failureReason(ApiBusinessException exception) {
        if (exception.getCode() == CommonApiCode.AUTH_ACCOUNT_DISABLED) {
            return FailureReason.ACCOUNT_DISABLED;
        }
        if (exception.getCode() == CommonApiCode.AUTH_FORBIDDEN) {
            return FailureReason.AUTH_FORBIDDEN;
        }
        if (exception.getCode() == CommonApiCode.AUTH_REQUIRED) {
            return FailureReason.AUTH_REQUIRED;
        }
        return FailureReason.INTERNAL_ERROR;
    }

    private void recordAudit(AuthenticationAuditEvent event) {
        try {
            authenticationAuditRecorder.record(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Authentication audit recorder failed and was ignored: eventType={} result={} failureType={}",
                    event.eventType(), event.result(), exception.getClass().getSimpleName());
        }
    }
}
