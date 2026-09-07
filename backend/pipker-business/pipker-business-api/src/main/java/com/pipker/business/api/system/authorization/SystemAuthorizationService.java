/**
 * @file SystemAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description Aggregates the cached API permissions and role-owned page-menu projection for a system user.
 * @logic Gives SUPER_ADMIN every enabled API permission and visible menu, while ordinary users union API permissions and MENU assignments across their enabled roles.
 * @dependencies SystemAccountService, SystemAuthorizationMapper, SystemAuthorizationCache, SystemMenu
 * @index_tags rbac, authorization, system-menu, cache
 * @author holic512
 */
package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.common.model.SystemMenuNode;
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

/**
 * 系统 RBAC 授权查询服务。
 */
@Service
public class SystemAuthorizationService {

    /**
     * 框架超级管理员角色编码。
     */
    public static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final SystemAccountService systemAccountService;
    private final SystemAuthorizationMapper systemAuthorizationMapper;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /**
     * 创建授权服务。
     *
     * @param systemAccountService 系统账户服务
     * @param systemAuthorizationMapper 授权关联 Mapper
     * @param systemAuthorizationCache 授权本地缓存
     */
    public SystemAuthorizationService(
            SystemAccountService systemAccountService,
            SystemAuthorizationMapper systemAuthorizationMapper,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemAccountService = systemAccountService;
        this.systemAuthorizationMapper = systemAuthorizationMapper;
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

    /**
     * 从数据库加载单个用户的授权快照，由本地缓存负责复用和过期。
     *
     * @param userId 用户主键
     * @return 授权投影；账户不存在或禁用时返回 {@code null}
     */
    private SystemAuthorizationSnapshot loadSnapshot(long userId) {
        SystemUser user = systemAccountService.findById(userId);
        if (user == null || !user.isEnabled()) {
            return null;
        }

        List<String> roles = List.copyOf(systemAuthorizationMapper.findRoleCodesByUserId(userId));
        boolean superAdmin = roles.contains(SUPER_ADMIN_ROLE);
        List<String> permissions = superAdmin
                ? systemAuthorizationMapper.findAllEnabledPermissionCodes()
                : systemAuthorizationMapper.findPermissionCodesByUserId(userId);

        return new SystemAuthorizationSnapshot(
                SystemUserProfile.from(user),
                roles,
                List.copyOf(permissions),
                buildMenuTree(findMenusForUser(userId, superAdmin))
        );
    }

    /**
     * 返回全部启用且可见的菜单树，供角色路由配置页展示。
     *
     * @return 可配置的完整菜单树
     */
    public List<SystemMenuNode> findAllVisibleMenuTree() {
        return buildMenuTree(systemAuthorizationMapper.findAllVisibleMenus());
    }

    /**
     * 按角色菜单关联选出页面菜单，并将每个页面的可见父目录补入菜单树。
     *
     * @param userId 当前用户主键
     * @param superAdmin 是否自动拥有全部页面菜单
     * @return 已授权页面与其父目录的扁平菜单
     */
    private List<SystemMenu> findMenusForUser(long userId, boolean superAdmin) {
        List<SystemMenu> allVisibleMenus = systemAuthorizationMapper.findAllVisibleMenus();
        Map<Long, SystemMenu> menusById = new HashMap<>();
        for (SystemMenu menu : allVisibleMenus) {
            menusById.put(menu.id(), menu);
        }

        List<SystemMenu> pageMenus = superAdmin
                ? allVisibleMenus.stream().filter(menu -> "MENU".equals(menu.menuType())).toList()
                : systemAuthorizationMapper.findVisiblePageMenusByUserId(userId);
        Set<Long> includedMenuIds = new HashSet<>();
        for (SystemMenu selectedPageMenu : pageMenus) {
            SystemMenu current = menusById.get(selectedPageMenu.id());
            while (current != null && includedMenuIds.add(current.id())) {
                Long parentId = current.parentId();
                current = parentId == null ? null : menusById.get(parentId);
            }
        }
        return allVisibleMenus.stream()
                .filter(menu -> includedMenuIds.contains(menu.id()))
                .toList();
    }

    /**
     * 将有序扁平菜单组装为树，同时容忍关联数据中缺失父菜单的孤立节点。
     */
    private List<SystemMenuNode> buildMenuTree(List<SystemMenu> menus) {
        Map<Long, MutableMenuNode> nodes = new HashMap<>();
        for (SystemMenu menu : menus) {
            nodes.put(menu.id(), new MutableMenuNode(menu));
        }

        List<MutableMenuNode> roots = new ArrayList<>();
        for (MutableMenuNode node : nodes.values()) {
            Long parentId = node.menu.parentId();
            MutableMenuNode parent = parentId == null ? null : nodes.get(parentId);
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }

        Comparator<MutableMenuNode> comparator = Comparator
                .comparing((MutableMenuNode node) -> node.menu.sort(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(node -> node.menu.id());
        return roots.stream().sorted(comparator).map(node -> node.toImmutable(comparator)).toList();
    }

    /**
     * 构造菜单树期间使用的可变节点。
     */
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
                    menu.id(),
                    menu.parentId(),
                    menu.menuName(),
                    menu.menuType(),
                    menu.routePath(),
                    menu.routeName(),
                    menu.componentKey(),
                    menu.icon(),
                    menu.sort(),
                    immutableChildren
            );
        }
    }
}
