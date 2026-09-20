/**
 * @file ApiLogManagementService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供接口访问日志的时间范围概览、趋势补桶、接口聚合、明细筛选与详情查询。
 * @logic 统一校验保留期内的查询范围和白名单排序，数据库负责聚合，服务层负责跨数据库稳定的百分比、舍入与空时间桶。
 * @dependencies SystemApiAccessLogMapper、ApiAccessLogDiagnostics、MyBatis-Plus、PipkerLogProperties
 * @index_tags api-log、analytics、query、trend、slow-request
 * @author holic512
 */
package com.pipker.business.api.system.apilog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipker.business.api.common.mapper.SystemApiAccessLogMapper;
import com.pipker.business.api.common.model.ApiAccessLogAggregate;
import com.pipker.business.api.common.model.SystemApiAccessLog;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.CollectorStatus;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.EndpointSummary;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.LogDetail;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.LogSummary;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.Overview;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.OverviewSummary;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.PageResult;
import com.pipker.business.api.system.apilog.ApiLogManagementResponse.TrendPoint;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.log.api.ApiAccessLogDiagnostics;
import com.pipker.starter.log.config.PipkerLogProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 接口访问日志只读分析服务。 */
@Service
public class ApiLogManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration HOURLY_BUCKET_LIMIT = Duration.ofHours(48);
    private static final Set<String> ENDPOINT_SORTS = Set.of(
            "REQUEST_COUNT", "SUCCESS_RATE", "AVG_DURATION", "MAX_DURATION", "SLOW_COUNT"
    );

    private final SystemApiAccessLogMapper mapper;
    private final ApiAccessLogDiagnostics diagnostics;
    private final PipkerLogProperties properties;

    public ApiLogManagementService(
            SystemApiAccessLogMapper mapper,
            Optional<ApiAccessLogDiagnostics> diagnostics,
            PipkerLogProperties properties
    ) {
        this.mapper = mapper;
        this.properties = properties;
        this.diagnostics = diagnostics.orElseGet(() -> () -> new ApiAccessLogDiagnostics.Snapshot(
                0, Math.max(0, properties.getApiAccess().getQueueCapacity()), 0, 0
        ));
    }

    /** 查询概览与连续时间趋势。 */
    public Overview overview(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long slowThresholdMs,
            boolean includeMonitorEndpoints
    ) {
        TimeRange range = resolveRange(startTime, endTime);
        long threshold = resolveSlowThreshold(slowThresholdMs);
        ApiAccessLogAggregate.Summary aggregate = mapper.selectSummary(
                range.start(), range.end(), threshold, includeMonitorEndpoints
        );
        boolean hourly = !Duration.between(range.start(), range.end()).minus(HOURLY_BUCKET_LIMIT).isPositive();
        List<TrendPoint> trend = hourly
                ? hourlyTrend(range, threshold, includeMonitorEndpoints)
                : dailyTrend(range, threshold, includeMonitorEndpoints);
        ApiAccessLogDiagnostics.Snapshot snapshot = diagnostics.snapshot();
        return new Overview(
                range.start(), range.end(), hourly ? "HOUR" : "DAY", threshold,
                toOverviewSummary(aggregate),
                trend,
                new CollectorStatus(
                        snapshot.queueDepth(), snapshot.queueCapacity(),
                        snapshot.droppedSinceStartup(), snapshot.writeFailuresSinceStartup()
                )
        );
    }

    /** 查询按接口聚合的性能分页。 */
    public PageResult<EndpointSummary> endpointPage(
            int page,
            int pageSize,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long slowThresholdMs,
            String keyword,
            String sortBy,
            String sortDirection,
            boolean includeMonitorEndpoints
    ) {
        validatePage(page, pageSize);
        TimeRange range = resolveRange(startTime, endTime);
        long threshold = resolveSlowThreshold(slowThresholdMs);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedSort = normalizeEnum(sortBy, ENDPOINT_SORTS, "sortBy", "AVG_DURATION");
        String normalizedDirection = normalizeEnum(sortDirection, Set.of("ASC", "DESC"), "sortDirection", "DESC");
        IPage<ApiAccessLogAggregate.Endpoint> result = mapper.selectEndpointPage(
                new Page<>(page, pageSize), range.start(), range.end(), threshold,
                normalizedKeyword, normalizedKeyword == null ? null : "%" + normalizedKeyword + "%",
                normalizedSort, normalizedDirection, includeMonitorEndpoints
        );
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getRecords().stream().map(this::toEndpointSummary).toList());
    }

    /** 查询原始访问日志分页。 */
    public PageResult<LogSummary> logPage(
            int page,
            int pageSize,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            String httpMethod,
            Boolean success,
            Long minDurationMs,
            String routePattern,
            Boolean unresolvedRoute
    ) {
        validatePage(page, pageSize);
        TimeRange range = resolveRange(startTime, endTime);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedMethod = normalizeOptional(httpMethod);
        if (normalizedMethod != null) {
            normalizedMethod = normalizedMethod.toUpperCase(Locale.ROOT);
        }
        if (minDurationMs != null && minDurationMs < 0) {
            throw validationError("minDurationMs must not be negative");
        }
        String normalizedRoutePattern = normalizeOptional(routePattern);
        if (Boolean.TRUE.equals(unresolvedRoute) && normalizedRoutePattern != null) {
            throw validationError("routePattern and unresolvedRoute=true cannot be used together");
        }
        String finalMethod = normalizedMethod;
        LambdaQueryWrapper<SystemApiAccessLog> query = new LambdaQueryWrapper<SystemApiAccessLog>()
                .ge(SystemApiAccessLog::getOccurredAt, range.start())
                .le(SystemApiAccessLog::getOccurredAt, range.end())
                .eq(finalMethod != null, SystemApiAccessLog::getHttpMethod, finalMethod)
                .eq(success != null, SystemApiAccessLog::getSuccess, success)
                .ge(minDurationMs != null, SystemApiAccessLog::getDurationMs, minDurationMs)
                .eq(normalizedRoutePattern != null, SystemApiAccessLog::getRoutePattern, normalizedRoutePattern)
                .isNull(Boolean.TRUE.equals(unresolvedRoute), SystemApiAccessLog::getRoutePattern)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(SystemApiAccessLog::getRequestPath, normalizedKeyword)
                        .or().like(SystemApiAccessLog::getRoutePattern, normalizedKeyword)
                        .or().like(SystemApiAccessLog::getPurpose, normalizedKeyword)
                        .or().like(SystemApiAccessLog::getTraceId, normalizedKeyword))
                .orderByDesc(SystemApiAccessLog::getOccurredAt)
                .orderByDesc(SystemApiAccessLog::getId);
        Page<SystemApiAccessLog> result = mapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getRecords().stream().map(this::toLogSummary).toList());
    }

    /** 查询单条接口访问详情。 */
    public LogDetail detail(String rawId) {
        long id = parsePositiveId(rawId);
        SystemApiAccessLog log = mapper.selectById(id);
        if (log == null) {
            throw new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, "API access log does not exist");
        }
        return toLogDetail(log);
    }

    private List<TrendPoint> hourlyTrend(TimeRange range, long threshold, boolean includeMonitorEndpoints) {
        Map<LocalDateTime, ApiAccessLogAggregate.HourlyTrend> rows = new HashMap<>();
        mapper.selectHourlyTrend(range.start(), range.end(), threshold, includeMonitorEndpoints)
                .forEach(row -> rows.put(row.getBucketStart(), row));
        LocalDateTime current = range.start().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime last = range.end().truncatedTo(ChronoUnit.HOURS);
        List<TrendPoint> result = new ArrayList<>();
        while (!current.isAfter(last)) {
            ApiAccessLogAggregate.HourlyTrend row = rows.get(current);
            result.add(row == null ? emptyTrend(current) : trend(
                    current, row.getTotalCount(), row.getSuccessCount(), row.getSlowCount(), row.getAvgDurationMs()
            ));
            current = current.plusHours(1);
        }
        return List.copyOf(result);
    }

    private List<TrendPoint> dailyTrend(TimeRange range, long threshold, boolean includeMonitorEndpoints) {
        Map<LocalDate, ApiAccessLogAggregate.DailyTrend> rows = new HashMap<>();
        mapper.selectDailyTrend(range.start(), range.end(), threshold, includeMonitorEndpoints)
                .forEach(row -> rows.put(row.getBucketDate(), row));
        LocalDate current = range.start().toLocalDate();
        LocalDate last = range.end().toLocalDate();
        List<TrendPoint> result = new ArrayList<>();
        while (!current.isAfter(last)) {
            ApiAccessLogAggregate.DailyTrend row = rows.get(current);
            LocalDateTime bucket = current.atStartOfDay();
            result.add(row == null ? emptyTrend(bucket) : trend(
                    bucket, row.getTotalCount(), row.getSuccessCount(), row.getSlowCount(), row.getAvgDurationMs()
            ));
            current = current.plusDays(1);
        }
        return List.copyOf(result);
    }

    private TrendPoint trend(LocalDateTime bucket, long total, long successes, long slow, double average) {
        return new TrendPoint(bucket, total, successes, total - successes, rate(successes, total), round(average), slow);
    }

    private TrendPoint emptyTrend(LocalDateTime bucket) {
        return new TrendPoint(bucket, 0, 0, 0, 0, 0, 0);
    }

    private OverviewSummary toOverviewSummary(ApiAccessLogAggregate.Summary aggregate) {
        long total = aggregate == null ? 0 : aggregate.getTotalCount();
        long successes = aggregate == null ? 0 : aggregate.getSuccessCount();
        return new OverviewSummary(
                total, successes, total - successes, rate(successes, total),
                aggregate == null ? 0 : round(aggregate.getAvgDurationMs()),
                aggregate == null ? 0 : aggregate.getMaxDurationMs(),
                aggregate == null ? 0 : aggregate.getSlowCount()
        );
    }

    private EndpointSummary toEndpointSummary(ApiAccessLogAggregate.Endpoint row) {
        return new EndpointSummary(
                row.getHttpMethod(), "<unresolved>".equals(row.getRoutePattern()) ? null : row.getRoutePattern(),
                row.getPurpose(), row.getTotalCount(), row.getSuccessCount(),
                row.getTotalCount() - row.getSuccessCount(), rate(row.getSuccessCount(), row.getTotalCount()),
                round(row.getAvgDurationMs()), row.getMaxDurationMs(), row.getSlowCount()
        );
    }

    private LogSummary toLogSummary(SystemApiAccessLog log) {
        return new LogSummary(
                String.valueOf(log.getId()), log.getOccurredAt(), log.getHttpMethod(), log.getRequestPath(),
                log.getRoutePattern(), log.getPurpose(), log.getHttpStatus(), log.getBusinessCode(),
                Boolean.TRUE.equals(log.getSuccess()), log.getHandlerDurationMs(), log.getDurationMs(), log.getTraceId()
        );
    }

    private LogDetail toLogDetail(SystemApiAccessLog log) {
        return new LogDetail(
                String.valueOf(log.getId()), log.getOccurredAt(), log.getServiceName(), log.getHttpMethod(),
                log.getRequestPath(), log.getRoutePattern(), log.getHandlerMethod(), log.getPurpose(), log.getRemark(),
                log.getHttpStatus(), log.getBusinessCode(), Boolean.TRUE.equals(log.getSuccess()),
                log.getHandlerDurationMs(), log.getDurationMs(), log.getTraceId(), log.getRequestId(), log.getClientIp(),
                log.getExceptionType(), log.getExceptionMessage()
        );
    }

    private TimeRange resolveRange(LocalDateTime start, LocalDateTime end) {
        if ((start == null) != (end == null)) {
            throw validationError("startTime and endTime must be provided together");
        }
        LocalDateTime actualEnd = end == null ? LocalDateTime.now() : end;
        LocalDateTime actualStart = start == null ? actualEnd.minusHours(24) : start;
        if (actualStart.isAfter(actualEnd)) {
            throw validationError("startTime must not be after endTime");
        }
        Duration retention = properties.getApiAccess().getRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalStateException("pipker.log.api-access.retention must be positive");
        }
        if (Duration.between(actualStart, actualEnd).compareTo(retention) > 0) {
            throw validationError("time range must not exceed the configured retention period");
        }
        return new TimeRange(actualStart, actualEnd);
    }

    private long resolveSlowThreshold(Long value) {
        long threshold = value == null ? properties.getApiAccess().getSlowThreshold().toMillis() : value;
        if (threshold < 0) {
            throw validationError("slowThresholdMs must not be negative");
        }
        return threshold;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw validationError("page must be positive and pageSize must be between 1 and 100");
        }
    }

    private String normalizeEnum(String value, Set<String> allowed, String field, String defaultValue) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return defaultValue;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw validationError(field + " has an unsupported value");
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

    private long parsePositiveId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw validationError("logId must be a positive integer");
        }
    }

    private double rate(long success, long total) {
        return total == 0 ? 0 : round(success * 100.0 / total);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }
}
