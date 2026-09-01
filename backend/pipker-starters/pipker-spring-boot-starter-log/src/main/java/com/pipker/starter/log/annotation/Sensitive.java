/**
 * @file Sensitive.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 指定字段、Record 组件或方法参数在日志中采用的脱敏规则。
 * @logic 脱敏渲染器读取该注解并仅转换日志副本，不会修改原始业务对象。
 * @dependencies Java 标准库、SensitiveType
 * @index_tags log、sensitive、annotation
 * @author holic512
 */
package com.pipker.starter.log.annotation;

import com.pipker.starter.log.sensitive.SensitiveType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定字段、Record 组件、方法参数或方法返回值在日志中的脱敏类型。
 */
@Documented
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    /**
     * 返回需要应用的敏感数据类型。
     *
     * @return 脱敏类型，默认为密码
     */
    SensitiveType value() default SensitiveType.PASSWORD;
}
