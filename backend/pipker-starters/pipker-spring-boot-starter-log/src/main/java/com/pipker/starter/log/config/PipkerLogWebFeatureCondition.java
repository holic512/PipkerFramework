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

/**
 * 判断是否需要注册承载 Web 日志能力的共享 Filter。
 */
public class PipkerLogWebFeatureCondition implements Condition {

    /**
     * 当 Trace、普通请求日志或慢请求日志任一能力启用时匹配条件。
     *
     * @param context 条件评估上下文
     * @param metadata 被评估组件的元数据
     * @return 是否应注册 Web 日志 Filter
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return isEnabled(context, "pipker.log.trace.enabled", true)
                || isEnabled(context, "pipker.log.request.enabled", false)
                || isEnabled(context, "pipker.log.slow-request.enabled", true);
    }

    /**
     * 读取布尔配置，并在属性不存在时使用指定默认值。
     */
    private boolean isEnabled(ConditionContext context, String propertyName, boolean defaultValue) {
        String value = context.getEnvironment().getProperty(propertyName);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }
}
