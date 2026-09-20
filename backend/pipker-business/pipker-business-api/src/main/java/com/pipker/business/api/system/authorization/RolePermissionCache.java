/**
 * @file RolePermissionCache.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 维护启用角色到当前有效接口权限编码的进程内一级缓存。
 * @logic 全量读取角色权限关系，在局部构建不可变快照后原子发布；多角色权限按 Java 枚举顺序求并集，映射变化时清除用户授权快照。
 * @dependencies SystemRolePermissionMapper、PermissionRegistry、SystemAuthorizationCache、Java Concurrency
 * @index_tags rbac、role、permission、l1-cache、atomic-snapshot
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.mapper.SystemRolePermissionMapper;
import com.pipker.business.api.common.model.SystemRolePermissionMapping;
import com.pipker.business.api.system.permission.PermissionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** 启用角色接口权限的原子进程内快照。 */
@Component
public class RolePermissionCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolePermissionCache.class);

    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final PermissionRegistry permissionRegistry;
    private final SystemAuthorizationCache systemAuthorizationCache;
    private final AtomicReference<CacheSnapshot> snapshot = new AtomicReference<>(CacheSnapshot.empty());

    /** 创建角色权限一级缓存。 */
    public RolePermissionCache(
            SystemRolePermissionMapper systemRolePermissionMapper,
            PermissionRegistry permissionRegistry,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemRolePermissionMapper = systemRolePermissionMapper;
        this.permissionRegistry = permissionRegistry;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /**
     * 从数据库全量重建角色权限映射；多个刷新来源在此串行化。
     *
     * @return 新快照的公开状态
     */
    public synchronized CacheStatus refresh() {
        List<String> allPermissionCodes = List.copyOf(permissionRegistry.allCodes());
        Set<String> validPermissionCodes = Set.copyOf(allPermissionCodes);
        Map<String, LinkedHashSet<String>> mutableMappings = new LinkedHashMap<>();

        for (SystemRolePermissionMapping mapping : systemRolePermissionMapper.findEnabledRolePermissionMappings()) {
            String roleCode = mapping.getRoleCode();
            if (roleCode == null || roleCode.isBlank()) {
                throw new IllegalStateException("Enabled role permission mapping contains a blank role code");
            }
            LinkedHashSet<String> permissions = mutableMappings.computeIfAbsent(
                    roleCode,
                    ignored -> new LinkedHashSet<>()
            );
            String permissionCode = mapping.getPermissionCode();
            if (permissionCode != null && validPermissionCodes.contains(permissionCode)) {
                permissions.add(permissionCode);
            }
        }

        if (mutableMappings.containsKey(SystemAuthorizationService.SUPER_ADMIN_ROLE)) {
            mutableMappings.put(
                    SystemAuthorizationService.SUPER_ADMIN_ROLE,
                    new LinkedHashSet<>(allPermissionCodes)
            );
        }

        Map<String, Set<String>> immutableMappings = new LinkedHashMap<>();
        mutableMappings.forEach((roleCode, permissions) ->
                immutableMappings.put(roleCode, Set.copyOf(permissions)));
        Map<String, Set<String>> publishedMappings = Map.copyOf(immutableMappings);
        long effectivePermissionGrantCount = publishedMappings.values().stream()
                .mapToLong(Set::size)
                .sum();
        CacheSnapshot previous = snapshot.get();
        CacheSnapshot refreshed = new CacheSnapshot(
                Instant.now(),
                publishedMappings,
                effectivePermissionGrantCount
        );
        snapshot.set(refreshed);

        boolean changed = !previous.rolePermissions().equals(publishedMappings);
        if (changed) {
            systemAuthorizationCache.invalidateAllSnapshots();
        }
        LOGGER.info(
                "Role permission cache refreshed: enabledRoles={}, effectiveGrants={}, changed={}",
                publishedMappings.size(),
                effectivePermissionGrantCount,
                changed
        );
        return refreshed.status();
    }

    /**
     * 按当前不可变快照合并多个启用角色的有效权限，并保持枚举目录顺序。
     *
     * @param roleCodes 用户持有的启用角色编码
     * @return 有效接口权限编码并集
     */
    public List<String> resolvePermissions(Collection<String> roleCodes) {
        CacheSnapshot current = snapshot.get();
        Set<String> granted = new HashSet<>();
        for (String roleCode : roleCodes) {
            granted.addAll(current.rolePermissions().getOrDefault(roleCode, Set.of()));
        }
        return permissionRegistry.allCodes().stream().filter(granted::contains).toList();
    }

    /** 返回当前已发布快照的只读统计状态。 */
    public CacheStatus currentStatus() {
        return snapshot.get().status();
    }

    /** 可安全返回给管理端的缓存状态。 */
    public record CacheStatus(
            Instant refreshedAt,
            int enabledRoleCount,
            long effectivePermissionGrantCount
    ) {
    }

    /** 包含完整不可变映射的内部快照。 */
    private record CacheSnapshot(
            Instant refreshedAt,
            Map<String, Set<String>> rolePermissions,
            long effectivePermissionGrantCount
    ) {

        private static CacheSnapshot empty() {
            return new CacheSnapshot(Instant.EPOCH, Map.of(), 0);
        }

        private CacheStatus status() {
            return new CacheStatus(refreshedAt, rolePermissions.size(), effectivePermissionGrantCount);
        }
    }
}
