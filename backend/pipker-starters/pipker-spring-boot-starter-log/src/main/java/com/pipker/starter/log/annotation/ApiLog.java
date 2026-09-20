/**
 * @file ApiLog.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 为统一接口访问日志提供可选的接口用途和备注元数据。
 * @logic Controller 方法无论是否标注都会被采集；切面仅在标注存在时补充用途和备注，缺失字段稳定为空字符串。
 * @dependencies Java Annotation API
 * @index_tags log、api、annotation、metadata
 * @author holic512
 */
package com.pipker.starter.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标注一个 HTTP 接口的用途和补充备注。 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {

    /** @return 面向管理端展示的接口用途。 */
    String purpose() default "";

    /** @return 面向管理端展示的补充备注。 */
    String remark() default "";
}
