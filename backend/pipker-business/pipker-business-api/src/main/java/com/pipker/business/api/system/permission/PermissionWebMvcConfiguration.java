/**
 * @file PermissionWebMvcConfiguration.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 将基于 Permission 注解的接口权限拦截器注册到系统 API 路径。
 * @logic Sa-Token Filter 先保证受保护 API 已登录，本配置再只为 /api/** 的 MVC HandlerMethod 解析权限注解；匿名和未标注端点保持原有行为。
 * @dependencies PermissionAuthorizationInterceptor、Spring Web MVC
 * @index_tags permission、configuration、interceptor、rbac、spring-mvc
 * @author holic512
 */
package com.pipker.business.api.system.permission;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册系统接口权限拦截器。
 */
@Configuration(proxyBeanMethods = false)
public class PermissionWebMvcConfiguration implements WebMvcConfigurer {

    private final PermissionAuthorizationInterceptor permissionAuthorizationInterceptor;

    /** 创建 MVC 权限配置。 */
    public PermissionWebMvcConfiguration(PermissionAuthorizationInterceptor permissionAuthorizationInterceptor) {
        this.permissionAuthorizationInterceptor = permissionAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionAuthorizationInterceptor)
                .addPathPatterns("/api/**");
    }
}
