/**
 * @file RouteManagementService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_menu 路由分页、树形查询及受限配置更新。
 * @logic 路由结构只读，管理树保留任意深度父子关系并容错孤立或循环节点；配置更新仅影响展示字段，事务提交后失效全部本地授权快照。
 * @dependencies SystemMenuMapper、MyBatis-Plus、RouteManagementResponse、Spring Framework
 * @index_tags route、system-menu、pagination、tree、read-model、administration
 * @author holic512
 */
package com.pipker.business.api.system.route;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipker.business.api.common.mapper.SystemMenuMapper;
import com.pipker.business.api.common.model.SystemMenu;
import com.pipker.business.api.system.route.RouteManagementResponse.PageResult;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteDetail;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteSummary;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteTreeNode;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pipker.business.api.system.authorization.SystemAuthorizationCache;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 系统路由管理应用服务。 */
@Service
public class RouteManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_MENU_TYPES = Set.of("DIRECTORY", "MENU");
    private static final Set<String> VALID_STATUSES = Set.of("ENABLED", "DISABLED");

    private final SystemMenuMapper systemMenuMapper;

    private final SystemAuthorizationCache authorizationCache;

    /** 创建路由管理服务。 */
    public RouteManagementService(SystemMenuMapper systemMenuMapper, SystemAuthorizationCache authorizationCache) {
        this.systemMenuMapper = systemMenuMapper;
        this.authorizationCache = authorizationCache;
    }

    /** 按路径、菜单名称、路由名或页面索引分页查询所有已落库路由定义。 */
    public PageResult<RouteSummary> findRoutePage(
            int page,
            int pageSize,
            String keyword,
            String menuType,
            Boolean visible,
            String status
    ) {
        validatePage(page, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedMenuType = normalizeOptionalMenuType(menuType);
        String normalizedStatus = normalizeOptionalStatus(status);

        LambdaQueryWrapper<SystemMenu> query = new LambdaQueryWrapper<SystemMenu>()
                .eq(normalizedMenuType != null, SystemMenu::getMenuType, normalizedMenuType)
                .eq(visible != null, SystemMenu::isVisible, visible)
                .eq(normalizedStatus != null, SystemMenu::getStatus, normalizedStatus)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(SystemMenu::getMenuName, normalizedKeyword)
                        .or()
                        .like(SystemMenu::getRoutePath, normalizedKeyword)
                        .or()
                        .like(SystemMenu::getRouteName, normalizedKeyword)
                        .or()
                        .like(SystemMenu::getComponentKey, normalizedKeyword))
                .orderByAsc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId);
        Page<SystemMenu> routePage = systemMenuMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(
                routePage.getCurrent(),
                routePage.getSize(),
                routePage.getTotal(),
                routePage.getRecords().stream().map(this::toSummary).toList()
        );
    }

    /**
     * 返回完整的路由管理树。管理页不分页，以避免深层目录在跨页时丢失祖先或子级。
     * 孤立节点与循环关联会安全回退为根节点，不影响其余有效分支展示。
     */
    public List<RouteTreeNode> findRouteTree() {
        List<SystemMenu> menus = systemMenuMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .orderByAsc(SystemMenu::getSort)
                .orderByAsc(SystemMenu::getId));
        Map<Long, MutableRouteTreeNode> nodes = new HashMap<>();
        for (SystemMenu menu : menus) {
            nodes.put(menu.getId(), new MutableRouteTreeNode(menu));
        }
        List<MutableRouteTreeNode> roots = new ArrayList<>();
        for (SystemMenu menu : menus) {
            MutableRouteTreeNode node = nodes.get(menu.getId());
            MutableRouteTreeNode parent = menu.getParentId() == null ? null : nodes.get(menu.getParentId());
            if (parent == null || introducesCycle(node, parent, nodes)) {
                roots.add(node);
            } else {
                parent.children.add(node);
            }
        }
        return roots.stream().map(MutableRouteTreeNode::toImmutable).toList();
    }

    /** 查询一条路由定义及其父级目录信息。 */
    public RouteDetail findRouteDetail(String routeId) {
        SystemMenu route = requireRoute(routeId);
        SystemMenu parent = route.getParentId() == null
                ? null
                : systemMenuMapper.selectById(route.getParentId());
        return new RouteDetail(
                stringifyId(route.getId()),
                route.getParentId() == null ? null : stringifyId(route.getParentId()),
                parent == null ? null : parent.getMenuName(),
                route.getMenuName(),
                route.getMenuType(),
                route.getRoutePath(),
                route.getRouteName(),
                route.getComponentKey(),
                route.getIcon(),
                route.getSort(),
                route.isVisible(),
                route.getStatus(),
                requiresComponentIndex(route),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }

    /** 仅更新允许后台维护的字段，空图标必须显式写入 NULL。 */
    @Transactional
    public RouteDetail updateConfiguration(String routeId, RouteConfigurationRequest request) {
        SystemMenu route = requireRoute(routeId);
        if (request == null) {
            throw validationError("配置不能为空");
        }
        String name = normalizeOptional(request.menuName());
        String icon = normalizeOptional(request.icon());
        String status = normalizeOptionalStatus(request.status());
        if (name == null || name.length() > 100) {
            throw validationError("菜单名称不能为空且不能超过 100 字符");
        }
        if (icon != null && icon.length() > 100) {
            throw validationError("图标名称不能超过 100 字符");
        }
        if (request.sort() == null || request.sort() < 0 || request.visible() == null || status == null) {
            throw validationError("排序必须为非负整数，展示和启用状态不能为空");
        }
        int changed = systemMenuMapper.update(new LambdaUpdateWrapper<SystemMenu>()
                .eq(SystemMenu::getId, route.getId())
                .set(SystemMenu::getMenuName, name)
                .set(SystemMenu::getIcon, icon)
                .set(SystemMenu::getSort, request.sort())
                .set(SystemMenu::isVisible, request.visible())
                .set(SystemMenu::getStatus, status)
                .set(SystemMenu::getUpdatedAt, LocalDateTime.now()));
        if (changed != 1) {
            throw validationError("路由不存在或已发生变化，请重新读取");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authorizationCache.invalidateAllSnapshots();
            }
        });
        return findRouteDetail(routeId);
    }

    private RouteSummary toSummary(SystemMenu route) {
        return new RouteSummary(
                stringifyId(route.getId()),
                route.getParentId() == null ? null : stringifyId(route.getParentId()),
                route.getMenuName(),
                route.getMenuType(),
                route.getRoutePath(),
                route.getRouteName(),
                route.getComponentKey(),
                route.getIcon(),
                route.getSort(),
                route.isVisible(),
                route.getStatus(),
                requiresComponentIndex(route)
        );
    }

    private boolean introducesCycle(
            MutableRouteTreeNode node,
            MutableRouteTreeNode parent,
            Map<Long, MutableRouteTreeNode> nodes
    ) {
        Set<Long> visited = new HashSet<>();
        MutableRouteTreeNode current = parent;
        while (current != null && visited.add(current.menu.getId())) {
            if (current.menu.getId().equals(node.menu.getId())) {
                return true;
            }
            Long parentId = current.menu.getParentId();
            current = parentId == null ? null : nodes.get(parentId);
        }
        return false;
    }

    private final class MutableRouteTreeNode {

        private final SystemMenu menu;
        private final List<MutableRouteTreeNode> children = new ArrayList<>();

        private MutableRouteTreeNode(SystemMenu menu) {
            this.menu = menu;
        }

        private RouteTreeNode toImmutable() {
            return new RouteTreeNode(
                    stringifyId(menu.getId()),
                    menu.getParentId() == null ? null : stringifyId(menu.getParentId()),
                    menu.getMenuName(),
                    menu.getMenuType(),
                    menu.getRoutePath(),
                    menu.getRouteName(),
                    menu.getComponentKey(),
                    menu.getIcon(),
                    menu.getSort(),
                    menu.isVisible(),
                    menu.getStatus(),
                    requiresComponentIndex(menu),
                    children.stream().map(MutableRouteTreeNode::toImmutable).toList()
            );
        }
    }

    private SystemMenu requireRoute(String rawRouteId) {
        long routeId = parsePositiveId(rawRouteId, "routeId");
        SystemMenu route = systemMenuMapper.selectById(routeId);
        if (route == null) {
            throw validationError("路由不存在");
        }
        return route;
    }

    private boolean requiresComponentIndex(SystemMenu route) {
        return "MENU".equals(route.getMenuType());
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw validationError("page must be positive and pageSize must be between 1 and 100");
        }
    }

    private String normalizeOptionalMenuType(String menuType) {
        String normalized = normalizeOptional(menuType);
        if (normalized == null) {
            return null;
        }
        if (!VALID_MENU_TYPES.contains(normalized)) {
            throw validationError("menuType must be DIRECTORY or MENU");
        }
        return normalized;
    }

    private String normalizeOptionalStatus(String status) {
        String normalized = normalizeOptional(status);
        if (normalized == null) {
            return null;
        }
        if (!VALID_STATUSES.contains(normalized)) {
            throw validationError("status must be ENABLED or DISABLED");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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

    private String stringifyId(long id) {
        return String.valueOf(id);
    }

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }
}
