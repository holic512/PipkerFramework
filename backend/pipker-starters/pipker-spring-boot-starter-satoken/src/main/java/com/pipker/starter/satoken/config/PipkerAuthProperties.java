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

/**
 * 绑定 {@code pipker.security.auth} 下的认证路由和会话存储配置。
 */
@Validated
@ConfigurationProperties("pipker.security.auth")
public class PipkerAuthProperties {

    /**
     * Sa-Token 会话存储后端，默认为内存。
     */
    @NotNull
    private SessionStore sessionStore = SessionStore.MEMORY;

    /**
     * 需要经过认证检查的路径模式，默认保护 {@code /api/**}。
     */
    @NotEmpty
    private List<String> protectedPaths = new ArrayList<>(List.of("/api/**"));

    /**
     * 按 {@code METHOD path} 格式声明的匿名路由。
     */
    private List<String> publicRoutes = new ArrayList<>(List.of("GET /api/ping"));

    /**
     * 返回会话存储后端。
     *
     * @return 会话存储后端
     */
    public SessionStore getSessionStore() {
        return sessionStore;
    }

    /**
     * 设置会话存储后端。
     *
     * @param sessionStore 会话存储后端
     */
    public void setSessionStore(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * 返回受保护路径模式。
     *
     * @return 受保护路径模式列表
     */
    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    /**
     * 设置受保护路径模式。
     *
     * @param protectedPaths 受保护路径模式列表
     */
    public void setProtectedPaths(List<String> protectedPaths) {
        this.protectedPaths = protectedPaths;
    }

    /**
     * 返回匿名路由声明。
     *
     * @return 匿名路由列表
     */
    public List<String> getPublicRoutes() {
        return publicRoutes;
    }

    /**
     * 设置匿名路由声明。
     *
     * @param publicRoutes 匿名路由列表
     */
    public void setPublicRoutes(List<String> publicRoutes) {
        this.publicRoutes = publicRoutes;
    }

    /**
     * 判断 HTTP 方法和路径组成的路由是否被配置为匿名路由。
     *
     * @param method HTTP 方法
     * @param path 请求路径
     * @return 匹配配置时返回 {@code true}
     */
    public boolean isPublicRoute(String method, String path) {
        String requestRoute = method.toUpperCase(Locale.ROOT) + " " + path;
        return publicRoutes.stream().anyMatch(requestRoute::equals);
    }
}
