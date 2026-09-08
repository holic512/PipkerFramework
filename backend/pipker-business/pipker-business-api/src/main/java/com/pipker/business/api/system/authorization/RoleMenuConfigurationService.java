/**
 * @file RoleMenuConfigurationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供角色优先的页面菜单配置与关联数据校验。
 * @logic 使用角色、菜单和角色菜单专属 Mapper 替换旧关联 Mapper，并在事务内以雪花主键重建普通角色的菜单关联。
 * @dependencies SystemRoleMapper、SystemMenuMapper、SystemRoleMenuMapper、SystemAuthorizationService、Spring Transaction
 * @index_tags rbac、role-menu、administration、cache、mybatis-plus
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipker.business.api.common.mapper.SystemMenuMapper;
import com.pipker.business.api.common.mapper.SystemRoleMapper;
import com.pipker.business.api.common.mapper.SystemRoleMenuMapper;
import com.pipker.business.api.common.model.SystemMenu;
import com.pipker.business.api.common.model.SystemMenuNode;
import com.pipker.business.api.common.model.SystemRole;
import com.pipker.business.api.common.model.SystemRoleMenu;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 角色路由菜单配置服务。 */
@Service
public class RoleMenuConfigurationService {

    private final SystemRoleMapper systemRoleMapper;
    private final SystemMenuMapper systemMenuMapper;
    private final SystemRoleMenuMapper systemRoleMenuMapper;
    private final SystemAuthorizationService systemAuthorizationService;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /** 创建角色路由菜单配置服务。 */
    public RoleMenuConfigurationService(
            SystemRoleMapper systemRoleMapper,
            SystemMenuMapper systemMenuMapper,
            SystemRoleMenuMapper systemRoleMenuMapper,
            SystemAuthorizationService systemAuthorizationService,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemRoleMapper = systemRoleMapper;
        this.systemMenuMapper = systemMenuMapper;
        this.systemRoleMenuMapper = systemRoleMenuMapper;
        this.systemAuthorizationService = systemAuthorizationService;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /** @return 所有启用角色、其菜单选择和可配置菜单树。 */
    public RoleMenuConfiguration getConfiguration() {
        List<SystemMenuNode> menus = systemAuthorizationService.findAllVisibleMenuTree();
        List<Long> allPageMenuIds = findPageMenuIds(menus);
        Map<Long, List<Long>> menuIdsByRoleId = assignmentsByRoleId(
                systemRoleMenuMapper.findAllEnabledPageMenuAssignments()
        );

        List<RoleMenuConfiguration.RoleMenuConfigurationRole> roles = systemRoleMapper.selectList(
                        new LambdaQueryWrapper<SystemRole>()
                                .eq(SystemRole::getStatus, "ENABLED")
                                .orderByAsc(SystemRole::getSort)
                                .orderByAsc(SystemRole::getRoleCode))
                .stream()
                .map(role -> toConfigurationRole(role, allPageMenuIds, menuIdsByRoleId))
                .toList();
        return new RoleMenuConfiguration(roles, menus);
    }

    /** 全量替换一个普通启用角色的页面菜单。 */
    @Transactional
    public RoleMenuUpdateResult replaceRoleMenuAssignments(long roleId, List<Long> requestedMenuIds) {
        if (roleId <= 0) {
            throw validationError("roleId must be positive");
        }
        SystemRole role = systemRoleMapper.selectById(roleId);
        if (role == null || !role.isEnabled()) {
            throw validationError("角色不存在或已停用");
        }
        if (SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.getRoleCode())) {
            throw validationError("SUPER_ADMIN 自动拥有全部启用菜单，不能手工配置");
        }

        List<Long> menuIds = normalizeMenuIds(requestedMenuIds);
        if (!menuIds.isEmpty()) {
            Set<Long> validMenuIds = systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                            .in(SystemMenu::getId, menuIds)
                            .eq(SystemMenu::getStatus, "ENABLED")
                            .eq(SystemMenu::isVisible, true)
                            .eq(SystemMenu::getMenuType, "MENU"))
                    .stream()
                    .map(SystemMenu::getId)
                    .collect(Collectors.toSet());
            if (validMenuIds.size() != menuIds.size() || !validMenuIds.containsAll(menuIds)) {
                throw validationError("菜单必须是启用且可见的页面菜单");
            }
        }

        systemRoleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenu>()
                .eq(SystemRoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            SystemRoleMenu assignment = new SystemRoleMenu();
            assignment.setRoleId(roleId);
            assignment.setMenuId(menuId);
            systemRoleMenuMapper.insert(assignment);
        }
        systemAuthorizationCache.invalidateAllSnapshots();
        return new RoleMenuUpdateResult(roleId, menuIds);
    }

    private RoleMenuConfiguration.RoleMenuConfigurationRole toConfigurationRole(
            SystemRole role,
            List<Long> allPageMenuIds,
            Map<Long, List<Long>> menuIdsByRoleId
    ) {
        boolean allMenus = SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.getRoleCode());
        List<Long> menuIds = allMenus
                ? allPageMenuIds
                : menuIdsByRoleId.getOrDefault(role.getId(), List.of());
        return new RoleMenuConfiguration.RoleMenuConfigurationRole(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getSort(),
                allMenus,
                menuIds
        );
    }

    private Map<Long, List<Long>> assignmentsByRoleId(List<SystemRoleMenu> assignments) {
        Map<Long, List<Long>> assignmentsByRoleId = new HashMap<>();
        for (SystemRoleMenu assignment : assignments) {
            assignmentsByRoleId.computeIfAbsent(assignment.getRoleId(), ignored -> new ArrayList<>())
                    .add(assignment.getMenuId());
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
        if (requestedMenuIds == null || requestedMenuIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long menuId : requestedMenuIds) {
            if (menuId == null || menuId <= 0) {
                throw validationError("menuIds must contain positive IDs only");
            }
            uniqueIds.add(menuId);
        }
        return List.copyOf(uniqueIds);
    }

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }
}
