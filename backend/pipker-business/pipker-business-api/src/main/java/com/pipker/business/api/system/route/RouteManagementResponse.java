/**
 * @file RouteManagementResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义只读路由管理列表与详情的安全响应投影。
 * @logic 将数据库雪花主键作为字符串输出，并显式标示目录不需要页面索引、页面菜单需要组件索引。
 * @dependencies Java 标准库
 * @index_tags route、system-menu、response、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.route;

import java.time.LocalDateTime;
import java.util.List;

/** 路由管理响应模型集合。 */
public final class RouteManagementResponse {

    private RouteManagementResponse() {
    }

    /** 路由列表摘要，目录与页面菜单共用同一持久化模型。 */
    public record RouteSummary(
            String id,
            String parentId,
            String menuName,
            String menuType,
            String routePath,
            String routeName,
            String componentKey,
            String icon,
            Integer sort,
            boolean visible,
            String status,
            boolean componentIndexRequired
    ) {
    }

    /** 路由详情额外显示父级名称与审计时间，便于核验落库索引。 */
    public record RouteDetail(
            String id,
            String parentId,
            String parentName,
            String menuName,
            String menuType,
            String routePath,
            String routeName,
            String componentKey,
            String icon,
            Integer sort,
            boolean visible,
            String status,
            boolean componentIndexRequired,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /** 不暴露 MyBatis-Plus 内部字段的通用分页结果。 */
    public record PageResult<T>(long page, long pageSize, long total, List<T> records) {
    }
}
