/**
 * @file DefaultLogValueRenderer.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 将脱敏日志副本编码为受长度限制的 JSON 文本。
 * @logic 先委托 LogSanitizer 复制和掩码，再委托应用 JsonMapper 序列化；异常统一返回安全占位值。
 * @dependencies LogSanitizer、Jackson JsonMapper、SensitiveType
 * @index_tags log、sensitive、json
 * @author holic512
 */
package com.pipker.starter.log.sensitive;

import tools.jackson.databind.json.JsonMapper;

public class DefaultLogValueRenderer implements LogValueRenderer {

    private static final String UNAVAILABLE = "<unavailable>";

    private final LogSanitizer sanitizer;
    private final JsonMapper jsonMapper;

    public DefaultLogValueRenderer(LogSanitizer sanitizer, JsonMapper jsonMapper) {
        this.sanitizer = sanitizer;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String render(Object value, int maxLength) {
        return render(value, null, maxLength);
    }

    @Override
    public String render(Object value, SensitiveType explicitType, int maxLength) {
        try {
            String rendered = jsonMapper.writeValueAsString(sanitizer.sanitize(value, explicitType));
            return truncate(rendered, maxLength);
        } catch (RuntimeException exception) {
            return UNAVAILABLE;
        }
    }

    private String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "…";
    }
}
