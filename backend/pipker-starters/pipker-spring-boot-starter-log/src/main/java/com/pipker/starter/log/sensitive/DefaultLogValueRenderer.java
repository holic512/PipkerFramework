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

/**
 * 使用配置的 JSON 映射器输出脱敏日志值，并限制输出长度。
 */
public class DefaultLogValueRenderer implements LogValueRenderer {

    private static final String UNAVAILABLE = "<unavailable>";

    private final LogSanitizer sanitizer;
    private final JsonMapper jsonMapper;

    /**
     * 创建默认日志值渲染器。
     *
     * @param sanitizer 日志值脱敏器
     * @param jsonMapper JSON 映射器
     */
    public DefaultLogValueRenderer(LogSanitizer sanitizer, JsonMapper jsonMapper) {
        this.sanitizer = sanitizer;
        this.jsonMapper = jsonMapper;
    }

    /**
     * 脱敏并渲染指定值。
     *
     * @param value 待渲染的原始值
     * @param maxLength 输出文本的最大长度；非正数表示不截断
     * @return JSON 文本或安全占位值
     */
    @Override
    public String render(Object value, int maxLength) {
        return render(value, null, maxLength);
    }

    /**
     * 按显式敏感类型脱敏并渲染指定值。
     *
     * @param value 待渲染的原始值
     * @param explicitType 强制使用的敏感类型，可为空
     * @param maxLength 输出文本的最大长度；非正数表示不截断
     * @return JSON 文本或安全占位值
     */
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
