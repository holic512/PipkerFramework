/**
 * @file DatabaseApiAccessLogHandler.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 将日志 Starter 异步发布的接口访问元数据批量追加到 system_api_access_log。
 * @logic 按服务器时区生成小时与日期桶，在独立事务中逐条使用 MyBatis-Plus 插入；失败由异步发布器统一隔离和计数。
 * @dependencies ApiAccessLogHandler、SystemApiAccessLogMapper、Spring Transaction
 * @index_tags api-log、database、async、persistence、transaction
 * @author holic512
 */
package com.pipker.business.api.system.apilog;

import com.pipker.business.api.common.mapper.SystemApiAccessLogMapper;
import com.pipker.business.api.common.model.SystemApiAccessLog;
import com.pipker.starter.log.api.ApiAccessLogHandler;
import com.pipker.starter.log.api.ApiAccessLogRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 数据库接口访问日志批量处理器。 */
@Component
public class DatabaseApiAccessLogHandler implements ApiAccessLogHandler {

    private final SystemApiAccessLogMapper mapper;

    public DatabaseApiAccessLogHandler(SystemApiAccessLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(List<ApiAccessLogRecord> records) {
        for (ApiAccessLogRecord record : records) {
            mapper.insert(toEntity(record));
        }
    }

    private SystemApiAccessLog toEntity(ApiAccessLogRecord record) {
        LocalDateTime occurredAt = LocalDateTime.ofInstant(record.occurredAt(), ZoneId.systemDefault());
        SystemApiAccessLog entity = new SystemApiAccessLog();
        entity.setOccurredAt(occurredAt);
        entity.setOccurredHour(occurredAt.truncatedTo(ChronoUnit.HOURS));
        entity.setOccurredDate(occurredAt.toLocalDate());
        entity.setServiceName(record.serviceName());
        entity.setHttpMethod(record.httpMethod());
        entity.setRequestPath(record.requestPath());
        entity.setRoutePattern(record.routePattern());
        entity.setHandlerMethod(record.handlerMethod());
        entity.setPurpose(record.purpose());
        entity.setRemark(record.remark());
        entity.setHttpStatus(record.httpStatus());
        entity.setBusinessCode(record.businessCode());
        entity.setSuccess(record.success());
        entity.setHandlerDurationMs(record.handlerDurationMs());
        entity.setDurationMs(record.durationMs());
        entity.setTraceId(record.traceId());
        entity.setRequestId(record.requestId());
        entity.setClientIp(record.clientIp());
        entity.setExceptionType(record.exceptionType());
        entity.setExceptionMessage(record.exceptionMessage());
        return entity;
    }
}
