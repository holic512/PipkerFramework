/**
 * @file RolePermissionCacheLifecycle.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 负责角色接口权限一级缓存的启动加载和固定延迟刷新。
 * @logic 应用就绪前同步执行首次加载并在失败时阻止启动；运行期按配置周期刷新，失败时记录错误并保留最后一次成功快照。
 * @dependencies RolePermissionCache、PipkerAuthProperties、Spring Boot ApplicationRunner、Spring Scheduling
 * @index_tags rbac、permission、cache、startup、scheduler
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 角色权限缓存的应用生命周期入口。 */
@Component
public class RolePermissionCacheLifecycle implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionCacheLifecycle.class);

    private final RolePermissionCache rolePermissionCache;

    /** 创建缓存生命周期组件并校验刷新周期。 */
    public RolePermissionCacheLifecycle(
            RolePermissionCache rolePermissionCache,
            PipkerAuthProperties authProperties
    ) {
        if (authProperties.getRolePermissionCacheRefreshInterval().isZero()
                || authProperties.getRolePermissionCacheRefreshInterval().isNegative()) {
            throw new IllegalArgumentException("role-permission-cache-refresh-interval must be positive");
        }
        this.rolePermissionCache = rolePermissionCache;
    }

    /** 应用启动期间同步完成首次加载，异常直接交给 Spring Boot 阻止应用就绪。 */
    @Override
    public void run(ApplicationArguments args) {
        rolePermissionCache.refresh();
    }

    /** 按配置的固定延迟刷新本实例缓存，失败时保留旧快照并等待下一周期。 */
    @Scheduled(
            fixedDelayString = "${pipker.security.auth.role-permission-cache-refresh-interval:5m}",
            initialDelayString = "${pipker.security.auth.role-permission-cache-refresh-interval:5m}"
    )
    public void scheduledRefresh() {
        try {
            rolePermissionCache.refresh();
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled role permission cache refresh failed; retaining previous snapshot", exception);
        }
    }
}
