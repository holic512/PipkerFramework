/**
 * @file ApiAccessLogDiagnostics.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 暴露当前应用实例异步接口日志队列的运行诊断快照。
 * @logic 读取操作无副作用，计数仅覆盖本实例本次启动周期，供管理端展示数据完整性风险。
 * @dependencies Java Standard Library
 * @index_tags log、api、diagnostics、queue
 * @author holic512
 */
package com.pipker.starter.log.api;

/** 当前实例接口日志异步管道的只读诊断入口。 */
public interface ApiAccessLogDiagnostics {

    /** @return 当前诊断快照。 */
    Snapshot snapshot();

    /** 进程内诊断快照。 */
    record Snapshot(int queueDepth, int queueCapacity, long droppedSinceStartup, long writeFailuresSinceStartup) {
    }
}
