/**
 * @file MdcTaskDecorator.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 为调用方管理的异步执行器提供显式 MDC 上下文透传工具。
 * @logic 在任务提交时复制当前 MDC，在目标线程执行期间恢复该副本并在结束后还原原线程上下文。
 * @dependencies Spring TaskDecorator、SLF4J MDC
 * @index_tags log、mdc、async
 * @author holic512
 */
package com.pipker.starter.log.context;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

public final class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> callerContext = MDC.getCopyOfContextMap();
        return () -> {
            try (MdcContextScope ignored = MdcContextScope.activate(callerContext)) {
                runnable.run();
            }
        };
    }
}
