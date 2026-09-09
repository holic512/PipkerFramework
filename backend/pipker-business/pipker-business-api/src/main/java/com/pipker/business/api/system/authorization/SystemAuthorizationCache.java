/**
 * @file SystemAuthorizationCache.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Provides a process-local TTL cache for user authorization snapshots.
 * @logic Caches both successful and absent user snapshots, while interface permissions are resolved from annotated handlers and the snapshot's enum-filtered role permissions; cache state is never synchronized between application instances.
 * @dependencies Caffeine, PipkerAuthProperties, SystemAuthorizationSnapshot
 * @index_tags rbac, permission, cache, caffeine
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;

/**
 * 系统授权快照的进程内缓存。
 */
@Component
public class SystemAuthorizationCache {

    private final Cache<Long, Optional<SystemAuthorizationSnapshot>> snapshotCache;

    /**
     * 按应用授权缓存配置初始化本地用户授权快照缓存。
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
}
