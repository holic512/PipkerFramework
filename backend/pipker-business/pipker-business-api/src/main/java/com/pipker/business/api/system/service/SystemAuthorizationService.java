/**
 * @file SystemAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 聚合系统用户的实时角色、权限、菜单和开发路由数据。
 * @logic 普通用户读取关联授权，SUPER_ADMIN 集中读取全部启用权限和菜单，并将扁平菜单组装为有序树。
 * @dependencies SystemAccountService、SystemAuthorizationMapper、SystemMenu、Spring Framework
 * @index_tags rbac、authorization、system-menu
 * @author holic512
 */
package com.pipker.business.api.system.service;

import com.pipker.business.api.system.mapper.SystemAuthorizationMapper;
import com.pipker.business.api.system.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.model.SystemMenu;
import com.pipker.business.api.system.model.SystemMenuNode;
import com.pipker.business.api.system.model.SystemRouteManifestItem;
import com.pipker.business.api.system.model.SystemUser;
import com.pipker.business.api.system.model.SystemUserProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 创建授权服务。
     *
     * @param systemAccountService 系统账户服务
     * @param systemAuthorizationMapper 授权关联 Mapper
     */
    public SystemAuthorizationService(
            SystemAccountService systemAccountService,
            SystemAuthorizationMapper systemAuthorizationMapper
    ) {
        this.systemAccountService = systemAccountService;
        this.systemAuthorizationMapper = systemAuthorizationMapper;
    }

    /**
     * 查询用户的完整授权投影。
     *
     * @param userId 用户主键
     * @return 授权投影；账户不存在或禁用时返回 {@code null}
     */
    public SystemAuthorizationSnapshot findSnapshot(long userId) {
        SystemUser user = systemAccountService.findById(userId);
        if (user == null || !user.isEnabled()) {
            return null;
        }

        List<String> roles = List.copyOf(systemAuthorizationMapper.findRoleCodesByUserId(userId));
        boolean superAdmin = roles.contains(SUPER_ADMIN_ROLE);
        List<String> permissions = superAdmin
                ? systemAuthorizationMapper.findAllEnabledPermissionCodes()
                : systemAuthorizationMapper.findPermissionCodesByUserId(userId);
        List<SystemMenu> menus = superAdmin
                ? systemAuthorizationMapper.findAllVisibleMenus()
                : systemAuthorizationMapper.findVisibleMenusByUserId(userId);

        return new SystemAuthorizationSnapshot(
                SystemUserProfile.from(user),
                roles,
                List.copyOf(permissions),
                buildMenuTree(menus)
        );
    }

    /**
     * 查询框架当前全部可装载的动态路由。
     *
     * @return 开发辅助路由列表
     */
    public List<SystemRouteManifestItem> findRouteManifest() {
        return systemAuthorizationMapper.findAllVisibleMenus().stream()
                .filter(SystemMenu::isRouteMenu)
                .map(menu -> new SystemRouteManifestItem(
                        menu.routePath(),
                        menu.routeName(),
                        menu.componentKey(),
                        menu.permissionCode()
                ))
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
                    menu.permissionCode(),
                    immutableChildren
            );
        }
    }
}
