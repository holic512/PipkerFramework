/**
 * @file ApiAccessLogRecord.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 承载一次完整 HTTP API 生命周期产生的安全元数据快照。
 * @logic Filter 在请求完成后创建不可变记录并交给异步发布器，记录不包含请求参数、请求体、响应体或认证凭据。
 * @dependencies Java Standard Library
 * @index_tags log、api、access、record、observability
 * @author holic512
 */
package com.pipker.starter.log.api;

import java.time.Instant;

/** 一次 API 请求的不可变访问日志。 */
public record ApiAccessLogRecord(
        Instant occurredAt,
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
