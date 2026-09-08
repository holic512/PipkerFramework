/**
 * @file RoleMenuConfigurationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供角色优先的页面菜单配置与关联数据校验。
 * @logic 使用角色、菜单和角色菜单专属 Mapper 重建普通角色的完整页面路由关联；全部雪花 ID 在 HTTP 边界保持字符串，隐藏菜单也可授权但不会进入导航，目录和未配置页面索引的页面不能授权。
 * @dependencies SystemRoleMapper、SystemMenuMapper、SystemRoleMenuMapper、SystemAuthorizationService、Spring Transaction
 * @index_tags rbac、role-menu、administration、cache、mybatis-plus、route、route-guard、snowflake-id
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
        List<SystemMenuNode> menus = findAssignableMenuTree();
        List<Long> allPageMenuIds = findEnabledPageMenuIds();
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

    /** @return 角色页面权限弹窗可呈现的全部启用目录和页面树，包含隐藏导航页面。 */
    public List<SystemMenuNode> findAssignableMenuTree() {
        return systemAuthorizationService.findAllEnabledRoutePermissionTree();
    }

    /**
     * 返回一个角色当前的页面权限选择；停用角色可查看但不能保存，SUPER_ADMIN 始终显示全部页面。
     */
    public RoleMenuConfiguration.RoleMenuConfigurationRole findRoleMenuConfiguration(String rawRoleId) {
        long roleId = parsePositiveId(rawRoleId, "roleId");
        SystemRole role = systemRoleMapper.selectById(roleId);
        if (role == null) {
            throw validationError("角色不存在");
        }
        List<Long> allPageMenuIds = findEnabledPageMenuIds();
        boolean allMenus = SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.getRoleCode());
        List<Long> menuIds = allMenus
                ? allPageMenuIds
                : systemRoleMenuMapper.findEnabledPageMenuAssignmentsByRoleId(roleId).stream()
                        .map(SystemRoleMenu::getMenuId)
                        .toList();
        return toConfigurationRole(role, menuIds, allMenus);
    }

    /** 全量替换一个普通启用角色的页面菜单。 */
    @Transactional
    public RoleMenuUpdateResult replaceRoleMenuAssignments(String rawRoleId, List<String> requestedMenuIds) {
        long roleId = parsePositiveId(rawRoleId, "roleId");
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
                            .eq(SystemMenu::getMenuType, "MENU"))
                    .stream()
                    .filter(SystemMenu::isRouteMenu)
                    .map(SystemMenu::getId)
                    .collect(Collectors.toSet());
            if (validMenuIds.size() != menuIds.size() || !validMenuIds.containsAll(menuIds)) {
                throw validationError("页面必须启用且具备完整的路径、路由名和页面索引");
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
        return new RoleMenuUpdateResult(stringifyId(roleId), stringifyIds(menuIds));
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
        return toConfigurationRole(role, menuIds, allMenus);
    }

    private RoleMenuConfiguration.RoleMenuConfigurationRole toConfigurationRole(
            SystemRole role,
            List<Long> menuIds,
            boolean allMenus
    ) {
        return new RoleMenuConfiguration.RoleMenuConfigurationRole(
                stringifyId(role.getId()),
                role.getRoleCode(),
                role.getRoleName(),
                role.getStatus(),
                role.getSort(),
                allMenus,
                stringifyIds(menuIds)
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

    private List<Long> findEnabledPageMenuIds() {
        return systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                        .eq(SystemMenu::getStatus, "ENABLED")
                .eq(SystemMenu::getMenuType, "MENU")
                .orderByAsc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId))
                .stream()
                .filter(SystemMenu::isRouteMenu)
                .map(SystemMenu::getId)
                .toList();
    }

    private List<Long> normalizeMenuIds(List<String> requestedMenuIds) {
        if (requestedMenuIds == null || requestedMenuIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (String rawMenuId : requestedMenuIds) {
            uniqueIds.add(parsePositiveId(rawMenuId, "menuIds"));
        }
        return List.copyOf(uniqueIds);
    }

    private long parsePositiveId(String rawId, String fieldName) {
        try {
            long id = Long.parseLong(rawId);
            if (id <= 0) {
                throw validationError(fieldName + " must be a positive Long ID");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw validationError(fieldName + " must be a positive Long ID");
        }
    }

    private List<String> stringifyIds(List<Long> ids) {
        return ids.stream().map(this::stringifyId).toList();
    }

    private String stringifyId(long id) {
        return String.valueOf(id);
    }

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }
}
