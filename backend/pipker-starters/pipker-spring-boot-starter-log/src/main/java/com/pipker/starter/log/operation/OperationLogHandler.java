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

@FunctionalInterface
public interface OperationLogHandler {

    void handle(OperationLogRecord record);
}
