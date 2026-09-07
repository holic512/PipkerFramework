/**
 * @file SystemApiAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Implements the Sa-Token API authorization callback from database API-resource mappings and cached user RBAC snapshots.
 * @logic Accepts only SYSTEM identities, requires exactly one enabled MVC resource match, then checks its API permission in the cached role-permission union.
 * @dependencies ApiAuthorizationService, SystemAuthorizationCache, SystemAuthorizationMapper, SystemAuthorizationService, PathPattern
 * @index_tags rbac, api, authorization, filter, cache
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.SystemLoginTypes;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.starter.satoken.service.ApiAuthorizationService;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 系统数据库 API 授权服务。
 */
@Service
public class SystemApiAuthorizationService implements ApiAuthorizationService {

    private final SystemAuthorizationMapper systemAuthorizationMapper;
    private final SystemAuthorizationService systemAuthorizationService;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /**
     * 创建数据库 API 授权服务。
     *
     * @param systemAuthorizationMapper API 资源查询 Mapper
     * @param systemAuthorizationService 用户授权快照服务
     * @param systemAuthorizationCache 本地授权缓存
     */
    public SystemApiAuthorizationService(
            SystemAuthorizationMapper systemAuthorizationMapper,
            SystemAuthorizationService systemAuthorizationService,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemAuthorizationMapper = systemAuthorizationMapper;
        this.systemAuthorizationService = systemAuthorizationService;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /**
     * 只有唯一的数据库规则命中且用户持有对应 API 权限时允许访问。
     */
    @Override
    public boolean isAuthorized(LoginIdentity identity, String httpMethod, String requestPath) {
        if (!SystemLoginTypes.SYSTEM.equals(identity.loginType())
                || httpMethod == null || requestPath == null || !requestPath.startsWith("/")) {
            return false;
        }
        long userId;
        try {
            userId = Long.parseLong(identity.userId());
        } catch (NumberFormatException exception) {
            return false;
        }

        String method = httpMethod.toUpperCase(Locale.ROOT);
        PathContainer path = PathContainer.parsePath(requestPath);
        List<SystemAuthorizationCache.ApiRouteRule> matchedRules = systemAuthorizationCache
                .getApiRouteRules(systemAuthorizationMapper::findAllEnabledApiResources)
                .stream()
                .filter(rule -> rule.matches(method, path))
                .toList();
        if (matchedRules.size() != 1) {
            return false;
        }

        SystemAuthorizationSnapshot snapshot = systemAuthorizationService.findSnapshot(userId);
        return snapshot != null && snapshot.permissions().contains(matchedRules.getFirst().permissionCode());
    }
}
