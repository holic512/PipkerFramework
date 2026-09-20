/**
 * @file ApiLogManagementResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义接口访问日志概览、趋势、接口聚合、分页和详情的安全响应契约。
 * @logic 雪花 ID 统一输出字符串，百分比与平均耗时由服务层舍入，不暴露任何请求或响应内容。
 * @dependencies Java Time、Java Collections
 * @index_tags api-log、response、dashboard、pagination
 * @author holic512
 */
package com.pipker.business.api.system.apilog;

import java.time.LocalDateTime;
import java.util.List;

/** 接口访问日志管理响应集合。 */
public final class ApiLogManagementResponse {

    private ApiLogManagementResponse() {
    }

    public record CollectorStatus(
            int queueDepth,
            int queueCapacity,
            long droppedSinceStartup,
            long writeFailuresSinceStartup
    ) {
    }

    public record OverviewSummary(
            long totalCount,
            long successCount,
            long failureCount,
            double successRate,
            double avgDurationMs,
            long maxDurationMs,
            long slowCount
    ) {
    }

    public record TrendPoint(
            LocalDateTime bucketStart,
            long totalCount,
            long successCount,
            long failureCount,
            double successRate,
            double avgDurationMs,
            long slowCount
    ) {
    }

    public record Overview(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String bucket,
            long slowThresholdMs,
            OverviewSummary summary,
            List<TrendPoint> trend,
            CollectorStatus collector
    ) {
    }

    public record EndpointSummary(
            String httpMethod,
            String routePattern,
            String purpose,
            long totalCount,
            long successCount,
            long failureCount,
            double successRate,
            double avgDurationMs,
            long maxDurationMs,
            long slowCount
    ) {
    }

    public record LogSummary(
            String id,
            LocalDateTime occurredAt,
            String httpMethod,
            String requestPath,
            String routePattern,
            String purpose,
            int httpStatus,
            Integer businessCode,
            boolean success,
            Long handlerDurationMs,
            long durationMs,
            String traceId
    ) {
    }

    public record LogDetail(
            String id,
            LocalDateTime occurredAt,
            String serviceName,
            String httpMethod,
            String requestPath,
            String routePattern,
            String handlerMethod,
            String purpose,
            String remark,
            int httpStatus,
            Integer businessCode,
            boolean success,
            Long handlerDurationMs,
            long durationMs,
            String traceId,
            String requestId,
            String clientIp,
            String exceptionType,
            String exceptionMessage
    ) {
    }

    public record PageResult<T>(long page, long pageSize, long total, List<T> records) {
    }
}
