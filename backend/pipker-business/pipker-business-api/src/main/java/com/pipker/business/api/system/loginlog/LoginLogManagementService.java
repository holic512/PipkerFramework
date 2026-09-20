/**
 * @file LoginLogManagementService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供认证生命周期日志的只读筛选与分页查询。
 * @logic 校验分页、事件、结果和时间范围，按账号/IP/TraceId 组合搜索，并固定按发生时间和主键倒序返回安全投影。
 * @dependencies SystemLoginLogMapper、MyBatis-Plus、ApiBusinessException
 * @index_tags authentication、audit、login-log、query、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.loginlog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipker.business.api.common.mapper.SystemLoginLogMapper;
import com.pipker.business.api.common.model.SystemLoginLog;
import com.pipker.business.api.system.loginlog.LoginLogManagementResponse.LoginLogSummary;
import com.pipker.business.api.system.loginlog.LoginLogManagementResponse.PageResult;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.EventType;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.Result;
import com.pipker.business.common.exception.ApiBusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/** 登录日志只读查询服务。 */
@Service
public class LoginLogManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> EVENT_TYPES = Set.of(
            EventType.LOGIN.name(), EventType.LOGOUT.name(), EventType.AUTH_CHECK.name()
    );
    private static final Set<String> RESULTS = Set.of(Result.SUCCESS.name(), Result.FAILURE.name());

    private final SystemLoginLogMapper systemLoginLogMapper;

    /** 创建查询服务。 */
    public LoginLogManagementService(SystemLoginLogMapper systemLoginLogMapper) {
        this.systemLoginLogMapper = systemLoginLogMapper;
    }

    /** 按稳定条件分页查询登录日志。 */
    public PageResult<LoginLogSummary> findPage(
            int page,
            int pageSize,
            String keyword,
            String eventType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        validatePage(page, pageSize);
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw validationError("startTime must not be after endTime");
        }
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedEventType = normalizeEnum(eventType, EVENT_TYPES, "eventType");
        String normalizedResult = normalizeEnum(result, RESULTS, "result");

        LambdaQueryWrapper<SystemLoginLog> query = new LambdaQueryWrapper<SystemLoginLog>()
                .eq(normalizedEventType != null, SystemLoginLog::getEventType, normalizedEventType)
                .eq(normalizedResult != null, SystemLoginLog::getResult, normalizedResult)
                .ge(startTime != null, SystemLoginLog::getOccurredAt, startTime)
                .le(endTime != null, SystemLoginLog::getOccurredAt, endTime)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(SystemLoginLog::getUsername, normalizedKeyword)
                        .or().like(SystemLoginLog::getClientIp, normalizedKeyword)
                        .or().like(SystemLoginLog::getTraceId, normalizedKeyword))
                .orderByDesc(SystemLoginLog::getOccurredAt)
                .orderByDesc(SystemLoginLog::getId);

        Page<SystemLoginLog> logPage = systemLoginLogMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(
                logPage.getCurrent(),
                logPage.getSize(),
                logPage.getTotal(),
                logPage.getRecords().stream().map(this::toSummary).toList()
        );
    }

    private LoginLogSummary toSummary(SystemLoginLog log) {
        return new LoginLogSummary(
                String.valueOf(log.getId()),
                log.getUserId() == null ? null : String.valueOf(log.getUserId()),
                log.getUsername(),
                log.getEventType(),
                log.getResult(),
                log.getFailureReason(),
                log.getClientIp(),
                log.getUserAgent(),
                log.getHttpMethod(),
                log.getRequestPath(),
                log.getTraceId(),
                log.getOccurredAt()
        );
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw validationError("page must be positive and pageSize must be between 1 and 100");
        }
    }

    private String normalizeEnum(String value, Set<String> validValues, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!validValues.contains(normalized)) {
            throw validationError(fieldName + " has an unsupported value");
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

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }
}
