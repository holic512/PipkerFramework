/**
 * @file ApiAccessLogAspect.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 统一拦截 REST Controller 方法并向当前请求补充接口用途、方法签名、执行耗时和异常摘要。
 * @logic 切面不直接持久化；同步或异常结束后把安全元数据写入 Servlet 请求属性，由生命周期 Filter 最终生成唯一记录。
 * @dependencies Spring AOP、Spring Web、ApiLog、ApiAccessLogRequestAttributes
 * @index_tags log、api、aop、controller、timing
 * @author holic512
 */
package com.pipker.starter.log.api;

import com.pipker.starter.log.annotation.ApiLog;
import com.pipker.starter.log.config.PipkerLogProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/** REST Controller 接口元数据切面。 */
@Aspect
public class ApiAccessLogAspect {

    private final int maxExceptionMessageLength;
    private final String maskText;

    public ApiAccessLogAspect(PipkerLogProperties properties) {
        this.maxExceptionMessageLength = Math.max(0, properties.getException().getMaxMessageLength());
        this.maskText = properties.getSensitive().getMaskText();
    }

    /** 拦截全部 REST Controller 公共方法，原样返回结果或传播异常。 */
    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * *(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        ApiLog annotation = AnnotatedElementUtils.findMergedAnnotation(method, ApiLog.class);
        if (request != null) {
            request.setAttribute(ApiAccessLogRequestAttributes.HANDLER_METHOD, joinPoint.getSignature().toLongString());
            request.setAttribute(ApiAccessLogRequestAttributes.PURPOSE, annotation == null ? "" : annotation.purpose().trim());
            request.setAttribute(ApiAccessLogRequestAttributes.REMARK, annotation == null ? "" : annotation.remark().trim());
        }

        long startedAt = System.nanoTime();
        try {
            return joinPoint.proceed();
        } catch (Throwable exception) {
            if (request != null) {
                request.setAttribute(ApiAccessLogRequestAttributes.EXCEPTION_TYPE, exception.getClass().getName());
                request.setAttribute(ApiAccessLogRequestAttributes.EXCEPTION_MESSAGE,
                        ApiAccessLogMessageSanitizer.sanitize(
                                exception.getMessage(), maxExceptionMessageLength, maskText
                        ));
            }
            throw exception;
        } finally {
            if (request != null) {
                request.setAttribute(ApiAccessLogRequestAttributes.HANDLER_DURATION_MS,
                        Math.max(0, (System.nanoTime() - startedAt) / 1_000_000));
            }
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

}
