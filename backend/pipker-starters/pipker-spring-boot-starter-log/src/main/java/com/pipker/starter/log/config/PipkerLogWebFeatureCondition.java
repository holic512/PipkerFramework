/**
 * @file PipkerLogWebFeatureCondition.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 判断是否至少有一个需要 Servlet 请求生命周期的日志能力处于启用状态。
 * @logic 当 Trace、普通请求日志和慢请求均显式关闭时阻止共享 Filter 注册，避免无意义的 Web Bean。
 * @dependencies Spring Framework Condition API
 * @index_tags log、autoconfigure、condition
 * @author holic512
 */
package com.pipker.starter.log.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class PipkerLogWebFeatureCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return isEnabled(context, "pipker.log.trace.enabled", true)
                || isEnabled(context, "pipker.log.request.enabled", false)
                || isEnabled(context, "pipker.log.slow-request.enabled", true);
    }

    private boolean isEnabled(ConditionContext context, String propertyName, boolean defaultValue) {
        String value = context.getEnvironment().getProperty(propertyName);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
}
