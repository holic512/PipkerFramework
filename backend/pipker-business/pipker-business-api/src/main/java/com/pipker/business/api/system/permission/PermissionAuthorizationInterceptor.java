/**
 * @file PermissionAuthorizationInterceptor.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 在 MVC 请求进入标注接口前按当前用户的有效权限点执行统一授权校验。
 * @logic 仅处理 HandlerMethod，优先读取方法注解后回退到控制器类注解；未标注接口不追加权限限制，标注接口必须命中当前授权快照中的枚举权限编码。
 * @dependencies Permission、CurrentSystemAuthorizationService、ApiBusinessException、Spring Web MVC
 * @index_tags permission、interceptor、rbac、api-authorization、spring-mvc
 * @author holic512
 */
package com.pipker.business.api.system.permission;

import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 基于 {@link Permission} 的接口权限拦截器。
 */
@Component
public class PermissionAuthorizationInterceptor implements HandlerInterceptor {

    private final CurrentSystemAuthorizationService currentSystemAuthorizationService;

    /** 创建接口权限拦截器。 */
    public PermissionAuthorizationInterceptor(CurrentSystemAuthorizationService currentSystemAuthorizationService) {
        this.currentSystemAuthorizationService = currentSystemAuthorizationService;
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

        boolean granted = currentSystemAuthorizationService.currentSnapshot()
                .permissions()
                .contains(permission.value().getCode());
        if (!granted) {
            throw new ApiBusinessException(CommonApiCode.AUTH_FORBIDDEN);
        }
        return true;
    }
}
