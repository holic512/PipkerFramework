/**
 * @file RoleMenuConfigurationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Provides role-first page-menu configuration and validates every persisted role route assignment.
 * @logic Reads one shared visible menu tree, marks SUPER_ADMIN as automatic-all, replaces ordinary-role MENU assignments transactionally, and clears local authorization snapshots after a save.
 * @dependencies SystemAuthorizationMapper, SystemAuthorizationService, SystemAuthorizationCache, Spring Transaction
 * @index_tags rbac, role-menu, administration, cache
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.model.SystemMenuNode;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 角色路由菜单配置服务。
 */
@Service
public class RoleMenuConfigurationService {

    private final SystemAuthorizationMapper systemAuthorizationMapper;
    private final SystemAuthorizationService systemAuthorizationService;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /**
     * 创建角色路由菜单配置服务。
     */
    public RoleMenuConfigurationService(
            SystemAuthorizationMapper systemAuthorizationMapper,
            SystemAuthorizationService systemAuthorizationService,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemAuthorizationMapper = systemAuthorizationMapper;
        this.systemAuthorizationService = systemAuthorizationService;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /**
     * 获取角色优先的菜单授权配置。
     *
     * @return 所有启用角色、其菜单选择和可配置菜单树
     */
    public RoleMenuConfiguration getConfiguration() {
        List<SystemMenuNode> menus = systemAuthorizationService.findAllVisibleMenuTree();
        List<Long> allPageMenuIds = findPageMenuIds(menus);
        Map<Long, List<Long>> menuIdsByRoleId = assignmentsByRoleId(
                systemAuthorizationMapper.findAllEnabledRoleMenuAssignments()
        );

        List<RoleMenuConfiguration.RoleMenuConfigurationRole> roles = systemAuthorizationMapper.findAllEnabledRoles()
                .stream()
                .map(role -> toConfigurationRole(role, allPageMenuIds, menuIdsByRoleId))
                .toList();
        return new RoleMenuConfiguration(roles, menus);
    }

    /**
     * 全量替换一个普通启用角色的页面菜单。
     *
     * @param roleId 角色主键
     * @param requestedMenuIds 客户端请求的页面菜单主键
     * @return 已保存的去重菜单主键
     */
    @Transactional
    public RoleMenuUpdateResult replaceRoleMenuAssignments(long roleId, List<Long> requestedMenuIds) {
        if (roleId <= 0) {
            throw validationError("roleId must be positive");
        }
        SystemRole role = systemAuthorizationMapper.findRoleById(roleId);
        if (role == null || !role.isEnabled()) {
            throw validationError("角色不存在或已停用");
        }
        if (SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.roleCode())) {
            throw validationError("SUPER_ADMIN 自动拥有全部启用菜单，不能手工配置");
        }

        List<Long> menuIds = normalizeMenuIds(requestedMenuIds);
        if (!menuIds.isEmpty()) {
            Set<Long> validMenuIds = new HashSet<>(
                    systemAuthorizationMapper.findEnabledVisiblePageMenuIds(menuIds)
            );
            if (validMenuIds.size() != menuIds.size() || !validMenuIds.containsAll(menuIds)) {
                throw validationError("菜单必须是启用且可见的页面菜单");
            }
        }

        systemAuthorizationMapper.deleteRoleMenuAssignments(roleId);
        if (!menuIds.isEmpty()) {
            systemAuthorizationMapper.insertRoleMenuAssignments(roleId, menuIds);
        }
        systemAuthorizationCache.invalidateAllSnapshots();
        return new RoleMenuUpdateResult(roleId, menuIds);
    }

    private RoleMenuConfiguration.RoleMenuConfigurationRole toConfigurationRole(
            SystemRole role,
            List<Long> allPageMenuIds,
            Map<Long, List<Long>> menuIdsByRoleId
    ) {
        boolean allMenus = SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.roleCode());
        List<Long> menuIds = allMenus
                ? allPageMenuIds
                : menuIdsByRoleId.getOrDefault(role.id(), List.of());
        return new RoleMenuConfiguration.RoleMenuConfigurationRole(
                role.id(),
                role.roleCode(),
                role.roleName(),
                role.sort(),
                allMenus,
                menuIds
        );
    }

    private Map<Long, List<Long>> assignmentsByRoleId(List<SystemRoleMenuAssignment> assignments) {
        Map<Long, List<Long>> assignmentsByRoleId = new HashMap<>();
        for (SystemRoleMenuAssignment assignment : assignments) {
            assignmentsByRoleId.computeIfAbsent(assignment.roleId(), ignored -> new ArrayList<>())
                    .add(assignment.menuId());
        }
        assignmentsByRoleId.values().forEach(menuIds -> menuIds.sort(Comparator.naturalOrder()));
        return assignmentsByRoleId;
    }

    private List<Long> findPageMenuIds(List<SystemMenuNode> menus) {
        List<Long> pageMenuIds = new ArrayList<>();
        collectPageMenuIds(menus, pageMenuIds);
        return pageMenuIds;
    }

    private void collectPageMenuIds(List<SystemMenuNode> menus, List<Long> pageMenuIds) {
        for (SystemMenuNode menu : menus) {
            if ("MENU".equals(menu.type())) {
                pageMenuIds.add(menu.id());
            }
            collectPageMenuIds(menu.children(), pageMenuIds);
        }
    }

    private List<Long> normalizeMenuIds(List<Long> requestedMenuIds) {
        if (requestedMenuIds == null) {
            throw validationError("menuIds must not be null");
        }
        LinkedHashSet<Long> uniqueMenuIds = new LinkedHashSet<>();
        for (Long menuId : requestedMenuIds) {
            if (menuId == null || menuId <= 0) {
                throw validationError("menuIds must contain positive values");
            }
            uniqueMenuIds.add(menuId);
        }
        return uniqueMenuIds.stream().sorted().toList();
    }

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }
}
