/**
 * @file LogValueRenderer.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义日志安全值的序列化和长度截断接口。
 * @logic 渲染器先调用脱敏服务再输出 JSON 文本，任何渲染失败均退化为固定占位值。
 * @dependencies LogSanitizer、SensitiveType
 * @index_tags log、sensitive、json
 * @author holic512
 */
package com.pipker.starter.log.sensitive;

/**
 * 将日志值脱敏并序列化为受长度限制的文本。
 */
public interface LogValueRenderer {

    /**
     * 脱敏并渲染指定值。
     *
     * @param value 待渲染的原始值
     * @param maxLength 输出文本的最大长度；非正数表示不截断
     * @return JSON 文本或安全占位值
     */
    String render(Object value, int maxLength);

    /**
     * 按显式敏感类型脱敏并渲染指定值。
     *
     * @param value 待渲染的原始值
     * @param explicitType 强制使用的敏感类型，可为空
     * @param maxLength 输出文本的最大长度；非正数表示不截断
     * @return JSON 文本或安全占位值
     */
    String render(Object value, SensitiveType explicitType, int maxLength);
}
