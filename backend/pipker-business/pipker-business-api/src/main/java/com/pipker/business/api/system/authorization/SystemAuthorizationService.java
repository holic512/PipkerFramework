/**
 * @file SystemAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 聚合缓存的 API 权限、导航菜单与页面路由授权投影。
 * @logic 通过专属表 Mapper 读取角色、权限和菜单；SUPER_ADMIN 自动获得全部启用权限和页面路由，菜单展示仅由 visible 决定。
 * @dependencies SystemAccountService、SystemUserRoleMapper、SystemPermissionMapper、SystemMenuMapper、SystemAuthorizationCache
 * @index_tags rbac、authorization、system-menu、route、cache、mybatis-plus
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipker.business.api.common.mapper.SystemMenuMapper;
import com.pipker.business.api.common.mapper.SystemPermissionMapper;
import com.pipker.business.api.common.mapper.SystemUserRoleMapper;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.common.model.SystemMenu;
import com.pipker.business.api.common.model.SystemMenuNode;
import com.pipker.business.api.common.model.SystemPermission;
import com.pipker.business.api.common.model.SystemRouteDefinition;
import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserProfile;
import com.pipker.business.api.system.user.SystemAccountService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 系统 RBAC 授权查询服务。 */
@Service
public class SystemAuthorizationService {

    /** 框架超级管理员角色编码。 */
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final SystemAccountService systemAccountService;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemPermissionMapper systemPermissionMapper;
    private final SystemMenuMapper systemMenuMapper;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /** 创建授权服务。 */
    public SystemAuthorizationService(
            SystemAccountService systemAccountService,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemPermissionMapper systemPermissionMapper,
            SystemMenuMapper systemMenuMapper,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemAccountService = systemAccountService;
        this.systemUserRoleMapper = systemUserRoleMapper;
        this.systemPermissionMapper = systemPermissionMapper;
        this.systemMenuMapper = systemMenuMapper;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /**
     * 查询用户的完整授权投影。
     *
     * @param userId 用户主键
     * @return 授权投影；账户不存在或禁用时返回 {@code null}
     */
    public SystemAuthorizationSnapshot findSnapshot(long userId) {
        return systemAuthorizationCache.getSnapshot(userId, this::loadSnapshot);
    }

    private SystemAuthorizationSnapshot loadSnapshot(long userId) {
        SystemUser user = systemAccountService.findById(userId);
        if (user == null || !user.isEnabled()) {
            return null;
        }

        List<String> roles = List.copyOf(systemUserRoleMapper.findEnabledRoleCodesByUserId(userId));
        boolean superAdmin = roles.contains(SUPER_ADMIN_ROLE);
        List<String> permissions = superAdmin
                ? findAllEnabledPermissionCodes()
                : systemUserRoleMapper.findEnabledPermissionCodesByUserId(userId);
        List<SystemMenu> authorizedPageMenus = findAuthorizedPageMenus(userId, superAdmin);

        return new SystemAuthorizationSnapshot(
                SystemUserProfile.from(user),
                roles,
                List.copyOf(permissions),
                buildMenuTree(findVisibleMenusForUser(authorizedPageMenus)),
                toRouteDefinitions(authorizedPageMenus)
        );
    }

    /** @return 可用于只读投影的全部启用可见菜单树。 */
    public List<SystemMenuNode> findAllVisibleMenuTree() {
        return buildMenuTree(findAllEnabledVisibleMenus());
    }

    /** @return 供角色路由配置页授权页面访问（包括隐藏导航菜单）使用的全部启用菜单树。 */
    public List<SystemMenuNode> findAllEnabledMenuTree() {
        return buildMenuTree(systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getStatus, "ENABLED")
                .orderByAsc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId)));
    }

    private List<String> findAllEnabledPermissionCodes() {
        return systemPermissionMapper.selectList(new LambdaQueryWrapper<SystemPermission>()
                        .eq(SystemPermission::getStatus, "ENABLED")
                        .eq(SystemPermission::getPermissionType, "API")
                        .orderByAsc(SystemPermission::getPermissionCode))
                .stream()
                .map(SystemPermission::getPermissionCode)
                .toList();
    }

    private List<SystemMenu> findAllEnabledVisibleMenus() {
        return systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getStatus, "ENABLED")
                .eq(SystemMenu::isVisible, true)
                .orderByAsc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId));
    }

    private List<SystemMenu> findAllEnabledPageMenus() {
        return systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getStatus, "ENABLED")
                .eq(SystemMenu::getMenuType, "MENU")
                .orderByAsc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId));
    }

    private List<SystemMenu> findAuthorizedPageMenus(long userId, boolean superAdmin) {
        return superAdmin
                ? findAllEnabledPageMenus()
                : systemUserRoleMapper.findEnabledPageMenusByUserId(userId);
    }

    private List<SystemMenu> findVisibleMenusForUser(List<SystemMenu> authorizedPageMenus) {
        List<SystemMenu> allVisibleMenus = findAllEnabledVisibleMenus();
        Map<Long, SystemMenu> menusById = new HashMap<>();
        for (SystemMenu menu : allVisibleMenus) {
            menusById.put(menu.getId(), menu);
        }

        Set<Long> includedMenuIds = new HashSet<>();
        for (SystemMenu selectedPageMenu : authorizedPageMenus) {
            if (!selectedPageMenu.isVisible()) {
                continue;
            }
            SystemMenu current = menusById.get(selectedPageMenu.getId());
            while (current != null && includedMenuIds.add(current.getId())) {
                Long parentId = current.getParentId();
                current = parentId == null ? null : menusById.get(parentId);
            }
        }
        return allVisibleMenus.stream()
                .filter(menu -> includedMenuIds.contains(menu.getId()))
                .toList();
    }

    private List<SystemRouteDefinition> toRouteDefinitions(List<SystemMenu> authorizedPageMenus) {
        return authorizedPageMenus.stream()
                .filter(SystemMenu::isRouteMenu)
                .map(menu -> new SystemRouteDefinition(
                        String.valueOf(menu.getId()),
                        menu.getMenuName(),
                        menu.getRoutePath(),
                        menu.getRouteName(),
                        menu.getComponentKey()
                ))
                .toList();
    }

    /** 将有序扁平菜单组装为树，同时容忍关联数据中缺失父菜单的孤立节点。 */
    private List<SystemMenuNode> buildMenuTree(List<SystemMenu> menus) {
        Map<Long, MutableMenuNode> nodes = new HashMap<>();
        for (SystemMenu menu : menus) {
            nodes.put(menu.getId(), new MutableMenuNode(menu));
        }

        List<MutableMenuNode> roots = new ArrayList<>();
        for (MutableMenuNode node : nodes.values()) {
            Long parentId = node.menu.getParentId();
            MutableMenuNode parent = parentId == null ? null : nodes.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }

        Comparator<MutableMenuNode> comparator = Comparator
                .comparing((MutableMenuNode node) -> node.menu.getSort(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(node -> node.menu.getId());
        return roots.stream().sorted(comparator).map(node -> node.toImmutable(comparator)).toList();
    }

    /** 构造菜单树期间使用的可变节点。 */
    private static final class MutableMenuNode {

        private final SystemMenu menu;
        private final List<MutableMenuNode> children = new ArrayList<>();

        private MutableMenuNode(SystemMenu menu) {
            this.menu = menu;
        }

        private SystemMenuNode toImmutable(Comparator<MutableMenuNode> comparator) {
            List<SystemMenuNode> immutableChildren = children.stream()
                    .sorted(comparator)
                    .map(child -> child.toImmutable(comparator))
                    .toList();
            return new SystemMenuNode(
                    String.valueOf(menu.getId()),
                    menu.getParentId() == null ? null : String.valueOf(menu.getParentId()),
                    menu.getMenuName(),
                    menu.getMenuType(),
                    menu.getRoutePath(),
                    menu.getRouteName(),
                    menu.getComponentKey(),
                    menu.getIcon(),
                    menu.getSort(),
                    menu.isVisible(),
                    immutableChildren
            );
        }
    }
}
