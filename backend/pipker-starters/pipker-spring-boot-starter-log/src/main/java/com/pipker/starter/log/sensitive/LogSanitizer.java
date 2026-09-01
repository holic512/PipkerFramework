/**
 * @file LogSanitizer.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义将任意日志值转换为安全副本的公开扩展接口。
 * @logic 实现必须不修改原对象，且在无法读取对象时返回安全占位值而非向业务传播异常。
 * @dependencies SensitiveType
 * @index_tags log、sensitive、spi
 * @author holic512
 */
package com.pipker.starter.log.sensitive;

public interface LogSanitizer {

    Object sanitize(Object value);

    Object sanitize(Object value, SensitiveType explicitType);
}
