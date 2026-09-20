/**
 * @file ApiAccessLogHandler.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义异步接口访问日志的批量消费扩展点。
 * @logic 日志 Starter 负责有界缓冲和批量调度，应用可实现该接口写入数据库或其他受控后端。
 * @dependencies ApiAccessLogRecord、Java Collections
 * @index_tags log、api、handler、spi、batch
 * @author holic512
 */
package com.pipker.starter.log.api;

import java.util.List;

/** 批量消费接口访问日志。 */
@FunctionalInterface
public interface ApiAccessLogHandler {

    /**
     * 消费一批不可变接口访问记录。
     *
     * @param records 非空记录批次
     */
    void handle(List<ApiAccessLogRecord> records);
}
