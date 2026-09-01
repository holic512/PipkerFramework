/**
 * @file MdcContextScope.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 使用作用域方式建立并恢复 SLF4J MDC 上下文。
 * @logic 进入时保存线程原 MDC，退出时完整恢复，防止 Web 容器和线程池复用造成上下文污染。
 * @dependencies SLF4J MDC、Java 标准库
 * @index_tags log、mdc、threadlocal
 * @author holic512
 */
package com.pipker.starter.log.context;

import org.slf4j.MDC;

import java.util.Map;

/**
 * 以可关闭作用域的形式临时替换当前线程的 SLF4J MDC 上下文。
 */
public final class MdcContextScope implements AutoCloseable {

    private final Map<String, String> previous;

    private MdcContextScope(Map<String, String> values) {
        this.previous = MDC.getCopyOfContextMap();
        if (values == null || values.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(values);
        }
    }

    /**
     * 激活指定 MDC 值，并在作用域关闭时恢复进入前的上下文。
     *
     * @param values 要激活的 MDC 值；为空时清空当前 MDC
     * @return 可用于 try-with-resources 的上下文作用域
     */
    public static MdcContextScope activate(Map<String, String> values) {
        return new MdcContextScope(values);
    }

    /**
     * 恢复进入作用域前的 MDC 内容。
     */
    @Override
    public void close() {
        if (previous == null || previous.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(previous);
        }
    }
}
