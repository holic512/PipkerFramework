/**
 * @file OperationLogHandler.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义业务操作日志的可替换消费端 SPI。
 * @logic 默认实现写入 SLF4J；业务方可提供唯一实现以接入数据库、消息队列或观测系统。
 * @dependencies OperationLogRecord
 * @index_tags log、operation、spi
 * @author holic512
 */
package com.pipker.starter.log.operation;

/**
 * 消费一条已经完成脱敏的业务操作日志记录。
 */
@FunctionalInterface
public interface OperationLogHandler {

    /**
     * 处理业务操作日志记录。
     *
     * @param record 已脱敏的操作日志记录
     */
    void handle(OperationLogRecord record);
}
