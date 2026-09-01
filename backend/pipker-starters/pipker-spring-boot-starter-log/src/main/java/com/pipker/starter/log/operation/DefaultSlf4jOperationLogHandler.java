/**
 * @file DefaultSlf4jOperationLogHandler.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 使用独立 pipker.operation Logger 输出默认业务操作日志。
 * @logic 按配置等级写出已经脱敏的记录字段，不包含数据库和消息中间件副作用。
 * @dependencies OperationLogRecord、PipkerLogProperties、SLF4J
 * @index_tags log、operation、slf4j
 * @author holic512
 */
package com.pipker.starter.log.operation;

import com.pipker.starter.log.config.PipkerLogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

public class DefaultSlf4jOperationLogHandler implements OperationLogHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("pipker.operation");

    private final PipkerLogProperties properties;

    public DefaultSlf4jOperationLogHandler(PipkerLogProperties properties) {
        this.properties = properties;
    }

    @Override
    public void handle(OperationLogRecord record) {
        String message = "[OPERATION] module={} operation={} method={} path={} success={} cost={}ms "
                + "traceId={} requestId={} userId={} username={} clientIp={} params={} result={} exceptionType={} exceptionMessage={}";
        Object[] arguments = {
                record.module(), record.operation(), record.method(), record.requestPath(), record.success(), record.costMillis(),
                record.traceId(), record.requestId(), record.userId(), record.username(), record.clientIp(),
                record.parameters(), record.result(), record.exceptionType(), record.exceptionMessage()
        };
        logAt(properties.getOperation().getLevel(), message, arguments);
    }

    private void logAt(LogLevel level, String message, Object[] arguments) {
        LogLevel actualLevel = level == null ? LogLevel.INFO : level;
        switch (actualLevel) {
            case TRACE -> LOGGER.trace(message, arguments);
            case DEBUG -> LOGGER.debug(message, arguments);
            case INFO -> LOGGER.info(message, arguments);
            case WARN -> LOGGER.warn(message, arguments);
            case ERROR, FATAL -> LOGGER.error(message, arguments);
            case OFF -> {
            }
        }
    }
}
