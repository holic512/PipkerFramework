/**
 * @file LogContext.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 向其他技术模块暴露受控的请求日志上下文写入入口。
 * @logic 仅允许贡献者追加非保留 MDC 键，并在写入时去除换行以避免日志注入。
 * @dependencies LogMdcKeys、Java 标准库
 * @index_tags log、mdc、extension
 * @author holic512
 */
package com.pipker.starter.log.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 保存一次请求中由扩展模块补充的日志上下文。
 *
 * <p>写入时会忽略保留键、空值和空白值，并移除换行及截断过长内容，避免污染日志上下文。</p>
 */
public final class LogContext {

    private static final int MAX_VALUE_LENGTH = 512;

    private final Map<String, String> values;

    /**
     * 使用请求 Filter 已建立的基础上下文创建扩展上下文。
     *
     * @param initialValues 初始日志键值
     */
    public LogContext(Map<String, String> initialValues) {
        this.values = new LinkedHashMap<>(initialValues);
    }

    /**
     * 尝试追加一个自定义日志字段。
     *
     * @param key 日志字段名
     * @param value 字段值
     */
    public void put(String key, Object value) {
        if (key == null || key.isBlank() || value == null || LogMdcKeys.isReserved(key)) {
            return;
        }
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.put(key, normalized);
        }
    }

    /**
     * 返回当前上下文的不可变快照。
     *
     * @return 日志上下文快照
     */
    public Map<String, String> snapshot() {
        return Map.copyOf(values);
    }

    private String normalize(Object value) {
        String normalized = Objects.toString(value, "")
                .replace('\r', ' ')
                .replace('\n', ' ');
        return normalized.length() <= MAX_VALUE_LENGTH
                ? normalized
                : normalized.substring(0, MAX_VALUE_LENGTH) + "…";
    }
}
