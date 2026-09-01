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

public enum OperationLogRecordPolicy {
    DEFAULT,
    ENABLED,
    DISABLED
}
