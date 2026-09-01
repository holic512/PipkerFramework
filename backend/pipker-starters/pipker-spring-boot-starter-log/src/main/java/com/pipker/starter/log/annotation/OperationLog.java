/**
 * @file OperationLog.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 声明需要记录统一业务操作日志的方法。
 * @logic AOP 在方法成功或抛出异常后构造脱敏后的操作记录，并不会改变业务方法的返回值或异常语义。
 * @dependencies Spring AOP
 * @index_tags log、operation、annotation、aop
 * @author holic512
 */
package com.pipker.starter.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记录统一业务操作日志的方法。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /**
     * 返回业务模块名称。
     *
     * @return 模块名称
     */
    String module();

    /**
     * 返回当前业务操作名称。
     *
     * @return 操作名称
     */
    String operation();

    /**
     * 指定当前方法是否记录调用参数；使用 {@link OperationLogRecordPolicy#DEFAULT} 时继承全局配置。
     *
     * @return 参数记录策略
     */
    OperationLogRecordPolicy recordParameters() default OperationLogRecordPolicy.DEFAULT;

    /**
     * 指定当前方法是否记录返回值；使用 {@link OperationLogRecordPolicy#DEFAULT} 时继承全局配置。
     *
     * @return 返回值记录策略
     */
    OperationLogRecordPolicy recordResult() default OperationLogRecordPolicy.DEFAULT;
}
