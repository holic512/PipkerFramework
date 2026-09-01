/**
 * @file PipkerLogAutoConfiguration.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 条件化装配 Pipker 日志 Starter 的核心、Servlet/MVC 与 AOP 能力。
 * @logic 总开关控制全部功能；各嵌套配置按运行环境和功能开关注册单一 Filter、异常观察器与操作日志 Aspect。
 * @dependencies Spring Boot AutoConfiguration、Jackson、Spring MVC、Spring AOP
 * @index_tags log、starter、autoconfigure
 * @author holic512
 */
package com.pipker.starter.log.config;

import com.pipker.starter.log.exception.DefaultExceptionLogReporter;
import com.pipker.starter.log.exception.ExceptionLogReporter;
import com.pipker.starter.log.exception.ExceptionLoggingInterceptor;
import com.pipker.starter.log.operation.DefaultSlf4jOperationLogHandler;
import com.pipker.starter.log.operation.OperationLogAspect;
import com.pipker.starter.log.operation.OperationLogHandler;
import com.pipker.starter.log.sensitive.DefaultLogSanitizer;
import com.pipker.starter.log.sensitive.DefaultLogValueRenderer;
import com.pipker.starter.log.sensitive.LogSanitizer;
import com.pipker.starter.log.sensitive.LogValueRenderer;
import com.pipker.starter.log.web.PipkerRequestLoggingFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * 条件化装配日志 Starter 的核心、Servlet/MVC 和操作日志能力。
 */
@AutoConfiguration(afterName = "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@ConditionalOnProperty(prefix = "pipker.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PipkerLogProperties.class)
public class PipkerLogAutoConfiguration {

    /**
     * 注册默认日志脱敏器。
     *
     * @param properties 日志配置
     * @return 默认日志脱敏器
     */
    @Bean
    @ConditionalOnMissingBean
    public LogSanitizer logSanitizer(PipkerLogProperties properties) {
        return new DefaultLogSanitizer(properties);
    }

    /**
     * 注册默认日志值渲染器。
     *
     * @param logSanitizer 日志脱敏器
     * @param jsonMapper 应用的 JSON 映射器
     * @return 默认日志值渲染器
     */
    @Bean
    @ConditionalOnMissingBean
    public LogValueRenderer logValueRenderer(LogSanitizer logSanitizer, JsonMapper jsonMapper) {
        return new DefaultLogValueRenderer(logSanitizer, jsonMapper);
    }

    /**
     * Servlet 环境下的异常报告器和请求日志 Filter 配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({HttpServletRequest.class, OncePerRequestFilter.class})
    static class ServletLogConfiguration {

        /**
         * 注册默认 HTTP 异常报告器。
         *
         * @param properties 日志配置
         * @return 默认异常报告器
         */
        @Bean
        @ConditionalOnProperty(prefix = "pipker.log.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean
        ExceptionLogReporter exceptionLogReporter(PipkerLogProperties properties) {
            return new DefaultExceptionLogReporter(properties);
        }

        /**
         * 创建并注册共享的请求日志 Filter。
         *
         * @param properties 日志配置
         * @param environment Spring 环境
         * @param valueRenderer 日志值渲染器
         * @param jsonMapper JSON 映射器
         * @param contributors 请求日志上下文贡献者
         * @param exceptionLogReporter 异常报告器提供器
         * @return Filter 注册信息
         */
        @Bean
        @Conditional(PipkerLogWebFeatureCondition.class)
        FilterRegistrationBean<PipkerRequestLoggingFilter> pipkerRequestLoggingFilterRegistration(
                PipkerLogProperties properties,
                Environment environment,
                LogValueRenderer valueRenderer,
                JsonMapper jsonMapper,
                ObjectProvider<com.pipker.starter.log.context.LogContextContributor> contributors,
                ObjectProvider<ExceptionLogReporter> exceptionLogReporter
        ) {
            PipkerRequestLoggingFilter filter = new PipkerRequestLoggingFilter(
                    properties,
                    environment,
                    valueRenderer,
                    jsonMapper,
                    contributors.orderedStream().toList(),
                    exceptionLogReporter.getIfAvailable()
            );
            FilterRegistrationBean<PipkerRequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
            registration.setOrder(properties.getRequest().getFilterOrder());
            registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
            return registration;
        }
    }

    /**
     * Spring MVC 异常观察器配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(WebMvcConfigurer.class)
    @ConditionalOnProperty(prefix = "pipker.log.exception", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class MvcExceptionConfiguration {

        /**
         * 注册委托异常报告器的 MVC 拦截器。
         *
         * @param exceptionLogReporter 异常报告器
         * @return MVC 配置器
         */
        @Bean
        WebMvcConfigurer pipkerExceptionLoggingWebMvcConfigurer(ExceptionLogReporter exceptionLogReporter) {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    registry.addInterceptor(new ExceptionLoggingInterceptor(exceptionLogReporter));
                }
            };
        }
    }

    /**
     * AOP 操作日志处理器和切面配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ProceedingJoinPoint.class)
    @ConditionalOnProperty(prefix = "pipker.log.operation", name = "enabled", havingValue = "true", matchIfMissing = true)
    @EnableAspectJAutoProxy
    static class OperationLogConfiguration {

        /**
         * 注册默认的 SLF4J 操作日志处理器。
         *
         * @param properties 日志配置
         * @return 默认操作日志处理器
         */
        @Bean
        @ConditionalOnMissingBean(OperationLogHandler.class)
        OperationLogHandler defaultOperationLogHandler(PipkerLogProperties properties) {
            return new DefaultSlf4jOperationLogHandler(properties);
        }

        /**
         * 注册操作日志 AOP 切面。
         *
         * @param properties 日志配置
         * @param logSanitizer 日志脱敏器
         * @param valueRenderer 日志值渲染器
         * @param operationLogHandlers 操作日志处理器列表
         * @return 操作日志切面
         */
        @Bean
        OperationLogAspect operationLogAspect(
                PipkerLogProperties properties,
                LogSanitizer logSanitizer,
                LogValueRenderer valueRenderer,
                List<OperationLogHandler> operationLogHandlers
        ) {
            return new OperationLogAspect(properties, logSanitizer, valueRenderer, operationLogHandlers);
        }
    }
}
