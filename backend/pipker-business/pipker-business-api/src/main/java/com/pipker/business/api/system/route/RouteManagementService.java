/**
 * @file RouteManagementService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_menu 路由定义的只读分页筛选和详情查询。
 * @logic 以 system_menu 作为路径、路由名、页面索引和菜单展示状态的唯一数据源；目录无需组件索引，页面菜单由路由管理页明确展示其索引要求。
 * @dependencies SystemMenuMapper、MyBatis-Plus、RouteManagementResponse、Spring Framework
 * @index_tags route、system-menu、pagination、read-model、administration
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
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import org.springframework.stereotype.Service;

import java.util.Set;

/** 系统路由管理应用服务。 */
@Service
public class RouteManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_MENU_TYPES = Set.of("DIRECTORY", "MENU");
    private static final Set<String> VALID_STATUSES = Set.of("ENABLED", "DISABLED");

    private final SystemMenuMapper systemMenuMapper;

    /** 创建只读路由管理服务。 */
    public RouteManagementService(SystemMenuMapper systemMenuMapper) {
        this.systemMenuMapper = systemMenuMapper;
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
