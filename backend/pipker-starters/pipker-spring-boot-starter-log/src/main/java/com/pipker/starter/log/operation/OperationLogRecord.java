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
        /**
         * 记录生成时间。
         */
        Instant occurredAt,
        /**
         * 业务模块名称。
         */
        String module,
        /**
         * 业务操作名称。
         */
        String operation,
        /**
         * 被拦截方法的完整签名。
         */
        String method,
        /**
         * 请求路径。
         */
        String requestPath,
        /**
         * TraceId。
         */
        String traceId,
        /**
         * 请求 ID。
         */
        String requestId,
        /**
         * 操作人 ID。
         */
        String userId,
        /**
         * 操作人名称。
         */
        String username,
        /**
         * 客户端 IP 地址。
         */
        String clientIp,
        /**
         * 已脱敏的调用参数 JSON 文本。
         */
        String parameters,
        /**
         * 已脱敏的返回值 JSON 文本。
         */
        String result,
        /**
         * 方法执行耗时，单位为毫秒。
         */
        long costMillis,
        /**
         * 方法是否执行成功。
         */
        boolean success,
        /**
         * 异常类型的完整类名；成功时为空。
         */
        String exceptionType,
        /**
         * 受长度限制且已去除换行的异常消息；成功时为空。
         */
        String exceptionMessage
) {
}
