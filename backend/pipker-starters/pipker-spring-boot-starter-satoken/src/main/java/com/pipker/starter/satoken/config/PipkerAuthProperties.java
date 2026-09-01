/**
 * 文件：PipkerAuthProperties.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Sa-Token
 * 说明：从外部配置绑定 Pipker 的认证路由和会话存储设置。
 * 处理逻辑：选择唯一的会话后端，定义受保护路径集合以及按 HTTP 方法区分的匿名 API 路由。
 * 依赖：Spring Boot Configuration Properties、Jakarta Validation
 * 检索关键词：starter、sa-token、配置、认证、路由
 * 作者：holic512
 */
package com.pipker.starter.satoken.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Validated
@ConfigurationProperties("pipker.security.auth")
public class PipkerAuthProperties {

    @NotNull
    private SessionStore sessionStore = SessionStore.MEMORY;

    @NotEmpty
    private List<String> protectedPaths = new ArrayList<>(List.of("/api/**"));

    private List<String> publicRoutes = new ArrayList<>(List.of("GET /api/ping"));

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    public void setProtectedPaths(List<String> protectedPaths) {
        this.protectedPaths = protectedPaths;
    }

    public List<String> getPublicRoutes() {
        return publicRoutes;
    }

    public void setPublicRoutes(List<String> publicRoutes) {
        this.publicRoutes = publicRoutes;
    }

    public boolean isPublicRoute(String method, String path) {
        String requestRoute = method.toUpperCase(Locale.ROOT) + " " + path;
        return publicRoutes.stream().anyMatch(requestRoute::equals);
    }
}
