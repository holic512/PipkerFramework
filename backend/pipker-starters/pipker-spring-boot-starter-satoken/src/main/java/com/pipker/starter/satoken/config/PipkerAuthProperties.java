/**
 * @file PipkerAuthProperties.java
 * @project Pipker Framework
 * @module Pipker Sa-Token Starter
 * @description Binds session, route authentication, database-authorization, and local-cache settings.
 * @logic Selects one session store, identifies protected and anonymous routes, and configures both user snapshot expiration and role-permission cache refresh cadence.
 * @dependencies Spring Boot Configuration Properties, Jakarta Validation
 * @index_tags starter, sa-token, configuration, authentication, authorization, cache
 * @author holic512
 */
package com.pipker.starter.satoken.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
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
     * 是否要求业务层为已认证 API 提供数据库授权判定。
     */
    private boolean databaseAuthorizationRequired;

    /**
     * 用户授权快照的本地缓存有效期。
     */
    @NotNull
    private Duration authorizationCacheTtl = Duration.ofSeconds(60);

    /**
     * 每个本地授权缓存允许保留的最大条目数。
     */
    private long authorizationCacheMaximumSize = 10_000;

    /**
     * 启用角色接口权限一级缓存的固定刷新间隔。
     */
    @NotNull
    private Duration rolePermissionCacheRefreshInterval = Duration.ofMinutes(5);

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
     * 返回是否强制启用数据库 API 授权。
     *
     * @return 启用时必须提供授权服务
     */
    public boolean isDatabaseAuthorizationRequired() {
        return databaseAuthorizationRequired;
    }

    /**
     * 设置是否强制启用数据库 API 授权。
     *
     * @param databaseAuthorizationRequired 是否强制启用
     */
    public void setDatabaseAuthorizationRequired(boolean databaseAuthorizationRequired) {
        this.databaseAuthorizationRequired = databaseAuthorizationRequired;
    }

    /**
     * 返回本地授权缓存有效期。
     *
     * @return 本地缓存有效期
     */
    public Duration getAuthorizationCacheTtl() {
        return authorizationCacheTtl;
    }

    /**
     * 设置本地授权缓存有效期。
     *
     * @param authorizationCacheTtl 本地缓存有效期
     */
    public void setAuthorizationCacheTtl(Duration authorizationCacheTtl) {
        this.authorizationCacheTtl = authorizationCacheTtl;
    }

    /**
     * 返回每个授权缓存的最大条目数。
     *
     * @return 最大条目数
     */
    public long getAuthorizationCacheMaximumSize() {
        return authorizationCacheMaximumSize;
    }

    /**
     * 设置每个授权缓存的最大条目数。
     *
     * @param authorizationCacheMaximumSize 最大条目数
     */
    public void setAuthorizationCacheMaximumSize(long authorizationCacheMaximumSize) {
        this.authorizationCacheMaximumSize = authorizationCacheMaximumSize;
    }

    /**
     * 返回角色接口权限一级缓存的刷新间隔。
     *
     * @return 刷新间隔
     */
    public Duration getRolePermissionCacheRefreshInterval() {
        return rolePermissionCacheRefreshInterval;
    }

    /**
     * 设置角色接口权限一级缓存的刷新间隔。
     *
     * @param rolePermissionCacheRefreshInterval 刷新间隔
     */
    public void setRolePermissionCacheRefreshInterval(Duration rolePermissionCacheRefreshInterval) {
        this.rolePermissionCacheRefreshInterval = rolePermissionCacheRefreshInterval;
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
