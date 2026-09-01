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

public final class LogContext {

    private static final int MAX_VALUE_LENGTH = 512;

    private final Map<String, String> values;

    public LogContext(Map<String, String> initialValues) {
        this.values = new LinkedHashMap<>(initialValues);
    }

    public void put(String key, Object value) {
        if (key == null || key.isBlank() || value == null || LogMdcKeys.isReserved(key)) {
            return;
        }
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.put(key, normalized);
        }
    }

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
