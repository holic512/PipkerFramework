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

public interface LogValueRenderer {

    String render(Object value, int maxLength);

    String render(Object value, SensitiveType explicitType, int maxLength);
}
