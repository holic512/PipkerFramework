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

/**
 * 将任意日志值转换为不会修改原对象的安全副本。
 */
public interface LogSanitizer {

    /**
     * 按字段名和类型规则脱敏指定值。
     *
     * @param value 待处理的原始值
     * @return 可安全写入日志的副本
     */
    Object sanitize(Object value);

    /**
     * 使用显式敏感类型脱敏指定值。
     *
     * @param value 待处理的原始值
     * @param explicitType 强制使用的敏感类型，可为空
     * @return 可安全写入日志的副本
     */
    Object sanitize(Object value, SensitiveType explicitType);
}
