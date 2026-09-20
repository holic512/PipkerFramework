/**
 * @file RolePermissionCacheTests.java
 * @project Pipker Framework
 * @module Pipker Business API Tests
 * @description 验证角色接口权限一级缓存的全量加载、稳定合并、失败保留和原子发布行为。
 * @logic 使用受控 Mapper 响应构造旧、新快照，确认授权读取不回查数据库且刷新期间不会暴露半成品。
 * @dependencies JUnit Jupiter、AssertJ、Java Reflection、Java Concurrency
 * @index_tags test、rbac、permission、cache、concurrency
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.mapper.SystemRolePermissionMapper;
import com.pipker.business.api.common.model.SystemRolePermissionMapping;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.api.system.permission.PermissionRegistry;
import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolePermissionCacheTests {

    private AtomicInteger mapperCalls;
    private IntFunction<List<SystemRolePermissionMapping>> mapperResponse;
    private PermissionRegistry permissionRegistry;
    private RolePermissionCache rolePermissionCache;

    @BeforeEach
    void setUp() {
        mapperCalls = new AtomicInteger();
        mapperResponse = ignored -> List.of();
        permissionRegistry = new PermissionRegistry();
        permissionRegistry.load();
        rolePermissionCache = new RolePermissionCache(
                mapperProxy(),
                permissionRegistry,
                new SystemAuthorizationCache(new PipkerAuthProperties())
        );
    }

    @Test
    void loadsEveryEnabledRoleAndResolvesPermissionsWithoutFurtherDatabaseReads() {
        mapperResponse = ignored -> List.of(
                mapping("EMPTY_ROLE", null),
                mapping("ROLE_A", PermissionEnum.SYSTEM_ROUTE_VIEW.getCode()),
                mapping("ROLE_A", "retired:permission"),
                mapping("ROLE_B", PermissionEnum.SYSTEM_ROLE_MANAGE.getCode()),
                mapping(SystemAuthorizationService.SUPER_ADMIN_ROLE, null)
        );

        RolePermissionCache.CacheStatus status = rolePermissionCache.refresh();

        assertThat(status.enabledRoleCount()).isEqualTo(4);
        assertThat(status.effectivePermissionGrantCount())
                .isEqualTo(permissionRegistry.allCodes().size() + 2L);
        assertThat(rolePermissionCache.resolvePermissions(List.of("EMPTY_ROLE"))).isEmpty();
        assertThat(rolePermissionCache.resolvePermissions(List.of("ROLE_B", "ROLE_A", "ROLE_A")))
                .containsExactlyElementsOf(permissionRegistry.allCodes().stream()
                        .filter(Set.of(
                                PermissionEnum.SYSTEM_ROUTE_VIEW.getCode(),
                                PermissionEnum.SYSTEM_ROLE_MANAGE.getCode()
                        )::contains)
                        .toList());
        assertThat(rolePermissionCache.resolvePermissions(List.of(SystemAuthorizationService.SUPER_ADMIN_ROLE)))
                .containsExactlyElementsOf(permissionRegistry.allCodes());
        assertThat(mapperCalls).hasValue(1);
    }

    @Test
    void retainsLastSuccessfulSnapshotWhenRefreshFails() {
        mapperResponse = call -> {
            if (call == 0) {
                return List.of(mapping("ROLE_A", PermissionEnum.SYSTEM_ROUTE_VIEW.getCode()));
            }
            throw new IllegalStateException("database unavailable");
        };
        rolePermissionCache.refresh();
        RolePermissionCache.CacheStatus previous = rolePermissionCache.currentStatus();

        assertThatThrownBy(rolePermissionCache::refresh)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        assertThat(rolePermissionCache.currentStatus()).isEqualTo(previous);
        assertThat(rolePermissionCache.resolvePermissions(List.of("ROLE_A")))
                .containsExactly(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode());
        assertThat(mapperCalls).hasValue(2);
    }

    @Test
    void publishesOnlyCompleteSnapshotsWhileARefreshIsInProgress() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch releaseRefresh = new CountDownLatch(1);
        mapperResponse = call -> {
            if (call == 0) {
                return List.of(mapping("ROLE_A", PermissionEnum.SYSTEM_ROUTE_VIEW.getCode()));
            }
            try {
                refreshStarted.countDown();
                if (!releaseRefresh.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test refresh timed out");
                }
                return List.of(mapping("ROLE_A", PermissionEnum.SYSTEM_ROLE_MANAGE.getCode()));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("test refresh interrupted", exception);
            }
        };
        rolePermissionCache.refresh();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<RolePermissionCache.CacheStatus> refresh = executor.submit(rolePermissionCache::refresh);
            assertThat(refreshStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(rolePermissionCache.resolvePermissions(List.of("ROLE_A")))
                    .containsExactly(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode());

            releaseRefresh.countDown();
            refresh.get(5, TimeUnit.SECONDS);
            assertThat(rolePermissionCache.resolvePermissions(List.of("ROLE_A")))
                    .containsExactly(PermissionEnum.SYSTEM_ROLE_MANAGE.getCode());
        } finally {
            releaseRefresh.countDown();
            executor.shutdownNow();
        }
    }

    private static SystemRolePermissionMapping mapping(String roleCode, String permissionCode) {
        SystemRolePermissionMapping mapping = new SystemRolePermissionMapping();
        mapping.setRoleCode(roleCode);
        mapping.setPermissionCode(permissionCode);
        return mapping;
    }

    private SystemRolePermissionMapper mapperProxy() {
        return (SystemRolePermissionMapper) Proxy.newProxyInstance(
                SystemRolePermissionMapper.class.getClassLoader(),
                new Class<?>[]{SystemRolePermissionMapper.class},
                (proxy, method, args) -> {
                    if ("findEnabledRolePermissionMappings".equals(method.getName())) {
                        return mapperResponse.apply(mapperCalls.getAndIncrement());
                    }
                    if ("toString".equals(method.getName())) {
                        return "RolePermissionMapperTestProxy";
                    }
                    throw new UnsupportedOperationException("Unexpected mapper method: " + method.getName());
                }
        );
    }
}
