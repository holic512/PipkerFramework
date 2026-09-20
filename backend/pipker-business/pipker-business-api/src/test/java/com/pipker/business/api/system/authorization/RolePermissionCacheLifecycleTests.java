/**
 * @file RolePermissionCacheLifecycleTests.java
 * @project Pipker Framework
 * @module Pipker Business API Tests
 * @description 验证角色权限缓存首次加载和运行期定时刷新采用不同的失败策略。
 * @logic 使用受控缓存替身确认启动异常向外传播、定时异常被隔离，并拒绝非正数刷新周期。
 * @dependencies JUnit Jupiter、AssertJ、PipkerAuthProperties
 * @index_tags test、permission、cache、startup、scheduler
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolePermissionCacheLifecycleTests {

    @Test
    void startupRefreshFailurePreventsSuccessfulRunnerCompletion() {
        StubRolePermissionCache cache = new StubRolePermissionCache();
        cache.failure = new IllegalStateException("database unavailable");
        RolePermissionCacheLifecycle lifecycle = new RolePermissionCacheLifecycle(
                cache,
                new PipkerAuthProperties()
        );

        assertThatThrownBy(() -> lifecycle.run(null))
                .isSameAs(cache.failure);
        assertThat(cache.refreshCalls).isEqualTo(1);
    }

    @Test
    void scheduledRefreshFailureIsContainedForTheNextRetry() {
        StubRolePermissionCache cache = new StubRolePermissionCache();
        cache.failure = new IllegalStateException("database unavailable");
        RolePermissionCacheLifecycle lifecycle = new RolePermissionCacheLifecycle(
                cache,
                new PipkerAuthProperties()
        );

        assertThatCode(lifecycle::scheduledRefresh).doesNotThrowAnyException();
        assertThat(cache.refreshCalls).isEqualTo(1);
    }

    @Test
    void rejectsNonPositiveRefreshIntervals() {
        PipkerAuthProperties properties = new PipkerAuthProperties();
        properties.setRolePermissionCacheRefreshInterval(Duration.ZERO);

        assertThatThrownBy(() -> new RolePermissionCacheLifecycle(new StubRolePermissionCache(), properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role-permission-cache-refresh-interval");
    }

    private static final class StubRolePermissionCache extends RolePermissionCache {

        private RuntimeException failure;
        private int refreshCalls;

        private StubRolePermissionCache() {
            super(null, null, null);
        }

        @Override
        public synchronized CacheStatus refresh() {
            refreshCalls++;
            if (failure != null) {
                throw failure;
            }
            return new CacheStatus(Instant.now(), 0, 0);
        }
    }
}
