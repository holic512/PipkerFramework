/**
 * @file SystemAuthorizationCache.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Provides process-local TTL caches for user authorization snapshots and protected API resource rules.
 * @logic Caches both successful and absent user snapshots, compiles API path templates once per refresh, and never synchronizes state between application instances.
 * @dependencies Caffeine, PipkerAuthProperties, SystemAuthorizationSnapshot, SystemApiResource
 * @index_tags rbac, cache, caffeine, api-resource
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 数据库授权数据的进程内缓存。
 */
@Component
public class SystemAuthorizationCache {

    private static final String API_ROUTE_RULES_CACHE_KEY = "all-enabled-api-route-rules";

    private final Cache<Long, Optional<SystemAuthorizationSnapshot>> snapshotCache;
    private final Cache<String, List<ApiRouteRule>> apiRouteRulesCache;

    /**
     * 按应用授权缓存配置初始化两个相同 TTL 的本地缓存。
     *
     * @param authProperties 认证与授权配置
     */
    public SystemAuthorizationCache(PipkerAuthProperties authProperties) {
        if (authProperties.getAuthorizationCacheTtl().isZero()
                || authProperties.getAuthorizationCacheTtl().isNegative()) {
            throw new IllegalArgumentException("authorization-cache-ttl must be positive");
        }
        if (authProperties.getAuthorizationCacheMaximumSize() <= 0) {
            throw new IllegalArgumentException("authorization-cache-maximum-size must be positive");
        }
        this.snapshotCache = Caffeine.newBuilder()
                .expireAfterWrite(authProperties.getAuthorizationCacheTtl())
                .maximumSize(authProperties.getAuthorizationCacheMaximumSize())
                .build();
        this.apiRouteRulesCache = Caffeine.newBuilder()
                .expireAfterWrite(authProperties.getAuthorizationCacheTtl())
                .maximumSize(1)
                .build();
    }

    /**
     * 获取或刷新一个用户的授权快照；不存在和禁用账户同样会缓存空结果。
     *
     * @param userId 用户主键
     * @param loader 数据库加载函数
     * @return 授权快照；账户不可用时为 {@code null}
     */
    public SystemAuthorizationSnapshot getSnapshot(
            long userId,
            Function<Long, SystemAuthorizationSnapshot> loader
    ) {
        return snapshotCache.get(userId, key -> Optional.ofNullable(loader.apply(key))).orElse(null);
    }

    /**
     * 获取或刷新全部启用 API 资源的已编译路径规则。
     *
     * @param loader 数据库资源加载函数
     * @return API 路由规则
     */
    public List<ApiRouteRule> getApiRouteRules(Supplier<List<SystemApiResource>> loader) {
        return apiRouteRulesCache.get(API_ROUTE_RULES_CACHE_KEY, ignored -> loader.get().stream()
                .map(ApiRouteRule::from)
                .toList());
    }

    /**
     * 清除一个用户的本地授权快照，供未来受控授权写入流程在本实例内立即生效。
     *
     * @param userId 用户主键
     */
    public void invalidateSnapshot(long userId) {
        snapshotCache.invalidate(userId);
    }

    /**
     * 清除本实例内全部用户授权快照，供角色级菜单配置保存后立即刷新受影响用户。
     */
    public void invalidateAllSnapshots() {
        snapshotCache.invalidateAll();
    }

    /**
     * 清除本实例缓存的 API 资源规则，供未来受控资源写入流程在本实例内立即生效。
     */
    public void invalidateApiRouteRules() {
        apiRouteRulesCache.invalidate(API_ROUTE_RULES_CACHE_KEY);
    }

    /**
     * 将数据库资源定义编译为可复用的 Spring MVC 模板匹配器。
     */
    public record ApiRouteRule(String permissionCode, String httpMethod, PathPattern pathPattern) {

        private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

        private static ApiRouteRule from(SystemApiResource resource) {
            return new ApiRouteRule(
                    resource.permissionCode(),
                    resource.httpMethod(),
                    PATH_PATTERN_PARSER.parse(resource.pathPattern())
            );
        }

        boolean matches(String method, PathContainer requestPath) {
            return httpMethod.equals(method) && pathPattern.matches(requestPath);
        }
    }
}
