/**
 * @file OperationLogRecordPolicy.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 定义操作日志注解对全局参数和返回值记录策略的覆盖方式。
 * @logic DEFAULT 继承配置属性，ENABLED 与 DISABLED 仅影响当前被标注的方法。
 * @dependencies Java 标准库
 * @index_tags log、operation、configuration
 * @author holic512
 */
package com.pipker.starter.log.annotation;

/**
 * 操作日志对参数和返回值记录策略的覆盖方式。
 */
public enum OperationLogRecordPolicy {

    /**
     * 继承 {@code pipker.log.operation} 下的全局配置。
     */
    DEFAULT,

    /**
     * 强制记录当前方法对应的值。
     */
    ENABLED,

    /**
     * 强制不记录当前方法对应的值。
     */
    DISABLED
}
