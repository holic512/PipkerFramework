/**
 * @file OperationLogAspect.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 拦截 @OperationLog 方法并在成功或失败后发布脱敏操作记录。
 * @logic 围绕通知保持原返回值与异常传播，仅将日志构造和 Handler 故障隔离为旁路行为。
 * @dependencies Spring AOP、OperationLogHandler、LogValueRenderer、SLF4J MDC
 * @index_tags log、operation、aop
 * @author holic512
 */
package com.pipker.starter.log.operation;

import com.pipker.starter.log.annotation.OperationLog;
import com.pipker.starter.log.annotation.OperationLogRecordPolicy;
import com.pipker.starter.log.annotation.Sensitive;
import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.context.LogMdcKeys;
import com.pipker.starter.log.sensitive.LogSanitizer;
import com.pipker.starter.log.sensitive.LogValueRenderer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拦截 {@link OperationLog} 方法并在成功或失败后发布操作日志记录。
 */
@Aspect
public class OperationLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLogAspect.class);

    private final PipkerLogProperties properties;
    private final LogSanitizer sanitizer;
    private final LogValueRenderer valueRenderer;
    private final List<OperationLogHandler> handlers;

    /**
     * 创建操作日志切面。
     *
     * @param properties 日志配置
     * @param sanitizer 日志值脱敏器
     * @param valueRenderer 日志值渲染器
     * @param handlers 操作日志处理器集合
     */
    public OperationLogAspect(
            PipkerLogProperties properties,
            LogSanitizer sanitizer,
            LogValueRenderer valueRenderer,
            List<OperationLogHandler> handlers
    ) {
        this.properties = properties;
        this.sanitizer = sanitizer;
        this.valueRenderer = valueRenderer;
        this.handlers = List.copyOf(handlers);
    }

    /**
     * 执行业务方法，并将成功结果或异常转换为旁路操作日志记录。
     *
     * @param joinPoint 当前 AOP 连接点
     * @param operationLog 当前方法上的操作日志声明
     * @return 业务方法原始返回值
     * @throws Throwable 业务方法原始抛出的异常
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startedAt = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            publish(createRecord(joinPoint, operationLog, startedAt, result, null));
            return result;
        } catch (Throwable exception) {
            publish(createRecord(joinPoint, operationLog, startedAt, null, exception));
            throw exception;
        }
    }

    private OperationLogRecord createRecord(
            ProceedingJoinPoint joinPoint,
            OperationLog operationLog,
            long startedAt,
            Object result,
            Throwable exception
    ) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        boolean recordParameters = resolvePolicy(operationLog.recordParameters(), properties.getOperation().isRecordParameters());
        boolean recordResult = resolvePolicy(operationLog.recordResult(), properties.getOperation().isRecordResult());
        long costMillis = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);

        return new OperationLogRecord(
                Instant.now(),
                operationLog.module(),
                operationLog.operation(),
                signature.toLongString(),
                MDC.get(LogMdcKeys.REQUEST_URI),
                MDC.get(LogMdcKeys.TRACE_ID),
                MDC.get(LogMdcKeys.REQUEST_ID),
                MDC.get("userId"),
                MDC.get("username"),
                MDC.get(LogMdcKeys.CLIENT_IP),
                recordParameters ? renderParameters(method, joinPoint.getArgs()) : null,
                recordResult ? valueRenderer.render(result, properties.getOperation().getMaxValueLength()) : null,
                costMillis,
                exception == null,
                exception == null ? null : exception.getClass().getName(),
                exception == null ? null : truncateMessage(exception.getMessage())
        );
    }

    private String renderParameters(Method method, Object[] arguments) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Parameter[] methodParameters = method.getParameters();
        for (int index = 0; index < arguments.length; index++) {
            Parameter parameter = index < methodParameters.length ? methodParameters[index] : null;
            String name = parameter != null && parameter.isNamePresent() ? parameter.getName() : "arg" + index;
            Sensitive sensitive = parameter == null ? null : parameter.getAnnotation(Sensitive.class);
            parameters.put(name, sensitive == null
                    ? arguments[index]
                    : sanitizer.sanitize(arguments[index], sensitive.value()));
        }
        return valueRenderer.render(parameters, properties.getOperation().getMaxValueLength());
    }

    private boolean resolvePolicy(OperationLogRecordPolicy policy, boolean defaultValue) {
        return switch (policy) {
            case DEFAULT -> defaultValue;
            case ENABLED -> true;
            case DISABLED -> false;
        };
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ');
        int maxLength = Math.max(0, properties.getException().getMaxMessageLength());
        return maxLength == 0 || normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength) + "…";
    }

    private void publish(OperationLogRecord record) {
        for (OperationLogHandler handler : handlers) {
            try {
                handler.handle(record);
            } catch (RuntimeException exception) {
                LOGGER.warn("Operation log handler failed and was ignored", exception);
            }
        }
    }
}
