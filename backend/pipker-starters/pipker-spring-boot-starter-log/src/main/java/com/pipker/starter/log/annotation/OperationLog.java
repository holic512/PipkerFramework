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

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    String module();

    String operation();

    OperationLogRecordPolicy recordParameters() default OperationLogRecordPolicy.DEFAULT;

    OperationLogRecordPolicy recordResult() default OperationLogRecordPolicy.DEFAULT;
}
