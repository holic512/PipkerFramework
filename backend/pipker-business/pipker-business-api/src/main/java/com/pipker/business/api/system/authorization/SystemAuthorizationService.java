/**
 * @file SystemAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 聚合缓存的接口权限、导航菜单与页面路由授权投影。
 * @logic 读取用户启用角色后从角色权限一级缓存求接口权限并集，菜单与路由继续通过专属 Mapper 构建；SUPER_ADMIN 由缓存授予全部当前枚举权限和全部有效页面路由，普通用户校验完整祖先状态与展示链。
 * @dependencies SystemAccountService、SystemUserRoleMapper、SystemMenuMapper、RolePermissionCache、SystemAuthorizationCache
 * @index_tags rbac、authorization、permission、system-menu、route、route-guard、cache、mybatis-plus
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipker.business.api.common.mapper.SystemMenuMapper;
import com.pipker.business.api.common.mapper.SystemUserRoleMapper;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.common.model.SystemMenu;
import com.pipker.business.api.common.model.SystemMenuNode;
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
    private final SystemMenuMapper systemMenuMapper;
    private final SystemAuthorizationCache systemAuthorizationCache;
    private final RolePermissionCache rolePermissionCache;

    /** 创建授权服务。 */
    public SystemAuthorizationService(
            SystemAccountService systemAccountService,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemMenuMapper systemMenuMapper,
            SystemAuthorizationCache systemAuthorizationCache,
            RolePermissionCache rolePermissionCache
    ) {
        this.systemAccountService = systemAccountService;
        this.systemUserRoleMapper = systemUserRoleMapper;
        this.systemMenuMapper = systemMenuMapper;
        this.systemAuthorizationCache = systemAuthorizationCache;
        this.rolePermissionCache = rolePermissionCache;
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
        List<String> permissions = rolePermissionCache.resolvePermissions(roles);
        List<SystemMenu> authorizedPageMenus = findAuthorizedPageMenus(userId, superAdmin);

        return new SystemAuthorizationSnapshot(
                SystemUserProfile.from(user),
                roles,
                List.copyOf(permissions),
                buildMenuTree(findVisibleMenusForUser(authorizedPageMenus, superAdmin)),
                toRouteDefinitions(authorizedPageMenus)
        );
    }

    /** 普通用户可见的有效菜单树。 */
    public List<SystemMenuNode> findAllVisibleMenuTree() {
        List<SystemMenu> menus = findAllMenus();
        Map<Long, SystemMenu> byId = indexMenus(menus);
        return buildMenuTree(menus.stream().filter(menu -> matchesAncestorChain(menu, byId, true)).toList());
    }

    /** 全部有效启用目录与页面，包括隐藏导航页面。 */
    public List<SystemMenuNode> findAllEnabledMenuTree() {
        return findAllEnabledRoutePermissionTree();
    }

    public List<SystemMenuNode> findAllEnabledRoutePermissionTree() {
        return findRoutePermissionTree(false);
    }

    /** 超管配置视图包含停用和隐藏页面，普通角色只包含有效启用页面。 */
    public List<SystemMenuNode> findRoutePermissionTree(boolean superAdmin) {
        List<SystemMenu> menus = findAllMenus();
        Map<Long, SystemMenu> byId = indexMenus(menus);
        return buildMenuTree(menus.stream()
                .filter(menu -> "DIRECTORY".equals(menu.getMenuType()) || menu.isRouteMenu())
                .filter(menu -> superAdmin || matchesAncestorChain(menu, byId, false))
                .toList());
    }

    /** 与角色保存校验共用的页面资格集合。 */
    public List<SystemMenu> findAvailablePageMenus(boolean superAdmin) {
        List<SystemMenu> menus = findAllMenus();
        Map<Long, SystemMenu> byId = indexMenus(menus);
        return menus.stream().filter(SystemMenu::isRouteMenu)
                .filter(menu -> superAdmin || matchesAncestorChain(menu, byId, false)).toList();
    }

    private List<SystemMenu> findAllMenus() {
        return systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .orderByAsc(SystemMenu::getSort).orderByAsc(SystemMenu::getId));
    }

    private Map<Long, SystemMenu> indexMenus(List<SystemMenu> menus) {
        Map<Long, SystemMenu> byId = new HashMap<>();
        menus.forEach(menu -> byId.put(menu.getId(), menu));
        return byId;
    }

    /** 缺失祖先或环形关联不生成普通用户可访问路由。 */
    private boolean matchesAncestorChain(SystemMenu menu, Map<Long, SystemMenu> byId, boolean visible) {
        Set<Long> visited = new HashSet<>();
        SystemMenu current = menu;
        while (current != null) {
            if (!visited.add(current.getId()) || !"ENABLED".equals(current.getStatus())
                    || (visible && !current.isVisible())) {
                return false;
            }
            if (current.getParentId() == null) {
                return true;
            }
            current = byId.get(current.getParentId());
        }
        return false;
    }

    private List<SystemMenu> findAuthorizedPageMenus(long userId, boolean superAdmin) {
        List<SystemMenu> available = findAvailablePageMenus(superAdmin);
        if (superAdmin) {
            return available;
        }
        Set<Long> assigned = new HashSet<>();
        systemUserRoleMapper.findEnabledPageMenusByUserId(userId)
                .forEach(menu -> assigned.add(menu.getId()));
        return available.stream().filter(menu -> assigned.contains(menu.getId())).toList();
    }

    private List<SystemMenu> findVisibleMenusForUser(List<SystemMenu> authorizedPages, boolean superAdmin) {
        List<SystemMenu> menus = findAllMenus();
        Map<Long, SystemMenu> byId = indexMenus(menus);
        Set<Long> included = new HashSet<>();
        for (SystemMenu page : authorizedPages) {
            if (!superAdmin && !matchesAncestorChain(page, byId, true)) {
                continue;
            }
            SystemMenu current = byId.get(page.getId());
            while (current != null && included.add(current.getId())) {
                current = current.getParentId() == null ? null : byId.get(current.getParentId());
            }
        }
        return menus.stream().filter(menu -> included.contains(menu.getId())).toList();
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
