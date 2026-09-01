/**
 * @file OperationLogRecord.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 承载已脱敏且可安全交给日志或外部存储处理器的业务操作记录。
 * @logic Aspect 在业务方法结束时创建不可变记录，Handler 只消费该副本而不接触原始参数和返回值。
 * @dependencies Java 标准库
 * @index_tags log、operation、record
 * @author holic512
 */
package com.pipker.starter.log.operation;

import java.time.Instant;

public record OperationLogRecord(
        Instant occurredAt,
        String module,
        String operation,
        String method,
        String requestPath,
        String traceId,
        String requestId,
        String userId,
        String username,
        String clientIp,
        String parameters,
        String result,
        long costMillis,
        boolean success,
        String exceptionType,
        String exceptionMessage
) {
}
