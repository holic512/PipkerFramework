/**
 * @file ApiAccessLoggingTests.java
 * @project Pipker Framework
 * @module Pipker Log Starter Tests
 * @description 验证接口注解切面、统一业务码判定、异步唯一完成、队列溢出和写入失败隔离。
 * @logic 使用 Mock Servlet 请求与真实有界发布器覆盖请求线程到批处理器的安全元数据链路，不依赖数据库。
 * @dependencies JUnit Jupiter、AssertJ、Mockito、Spring Mock Web
 * @index_tags test、api-access、aop、async、queue、business-code
 * @author holic512
 */
package com.pipker.starter.log;

import com.pipker.starter.log.annotation.ApiLog;
import com.pipker.starter.log.api.ApiAccessLogAspect;
import com.pipker.starter.log.api.ApiAccessLogDiagnostics;
import com.pipker.starter.log.api.ApiAccessLogRecord;
import com.pipker.starter.log.api.ApiAccessLogRequestAttributes;
import com.pipker.starter.log.api.AsyncApiAccessLogPublisher;
import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.sensitive.DefaultLogSanitizer;
import com.pipker.starter.log.sensitive.DefaultLogValueRenderer;
import com.pipker.starter.log.web.PipkerRequestLoggingFilter;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiAccessLoggingTests {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aspectAddsOptionalMetadataAndRethrowsSanitizedExceptions() throws Throwable {
        PipkerLogProperties properties = properties();
        properties.getException().setMaxMessageLength(100);
        ApiAccessLogAspect aspect = new ApiAccessLogAspect(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/aspect");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProceedingJoinPoint annotated = joinPoint("annotated", "ok", null);
        assertThat(aspect.around(annotated)).isEqualTo("ok");
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.PURPOSE)).isEqualTo("查询接口用途");
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.REMARK)).isEqualTo("补充说明");
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.HANDLER_METHOD)).isEqualTo("TestController.annotated()");
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.HANDLER_DURATION_MS)).isInstanceOf(Long.class);

        request = new MockHttpServletRequest("GET", "/api/aspect/defaults");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertThat(aspect.around(joinPoint("unannotated", "plain", null))).isEqualTo("plain");
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.PURPOSE)).isEqualTo("");
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.REMARK)).isEqualTo("");

        request = new MockHttpServletRequest("GET", "/api/aspect/failure");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        IllegalStateException failure = new IllegalStateException(
                "first line\npassword=plain-secret Authorization=Bearer abc.def"
        );
        ProceedingJoinPoint failing = joinPoint("annotated", null, failure);
        assertThatThrownBy(() -> aspect.around(failing)).isSameAs(failure);
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.EXCEPTION_TYPE))
                .isEqualTo(IllegalStateException.class.getName());
        assertThat(request.getAttribute(ApiAccessLogRequestAttributes.EXCEPTION_MESSAGE).toString())
                .doesNotContain("\n", "plain-secret", "abc.def")
                .contains("password=******", "Authorization=******");
    }

    @Test
    void filterUsesOnlyTopLevelBusinessCodeAndPublishesNoContent() throws Exception {
        PipkerLogProperties properties = properties();
        LinkedBlockingQueue<ApiAccessLogRecord> captured = new LinkedBlockingQueue<>();
        AsyncApiAccessLogPublisher publisher = new AsyncApiAccessLogPublisher(
                properties,
                List.of(records -> captured.addAll(records))
        );
        publisher.start();
        try {
            PipkerRequestLoggingFilter filter = filter(properties, publisher);
            filter.doFilter(request("/api/orders/1"), response(), (servletRequest, servletResponse) -> {
                servletRequest.setAttribute(ApiAccessLogRequestAttributes.PURPOSE, "查询订单");
                servletRequest.setAttribute(ApiAccessLogRequestAttributes.REMARK, "");
                servletRequest.setAttribute(ApiAccessLogRequestAttributes.HANDLER_METHOD, "OrderController.detail()");
                servletResponse.setContentType("application/json");
                servletResponse.getWriter().write("{\"data\":{\"code\":200},\"code\":400,\"password\":\"must-not-persist\"}");
            });
            ApiAccessLogRecord failed = captured.poll(1, TimeUnit.SECONDS);
            assertThat(failed).isNotNull();
            assertThat(failed.businessCode()).isEqualTo(400);
            assertThat(failed.success()).isFalse();
            assertThat(failed.purpose()).isEqualTo("查询订单");
            assertThat(failed.toString()).doesNotContain("must-not-persist").doesNotContain("password");

            filter.doFilter(request("/api/orders/2"), response(), (servletRequest, servletResponse) -> {
                servletResponse.setContentType("application/json");
                servletResponse.getWriter().write("{\"data\":{\"code\":400},\"code\":200}");
            });
            ApiAccessLogRecord succeeded = captured.poll(1, TimeUnit.SECONDS);
            assertThat(succeeded).isNotNull();
            assertThat(succeeded.businessCode()).isEqualTo(200);
            assertThat(succeeded.success()).isTrue();
            assertThat(succeeded.purpose()).isEmpty();
            assertThat(succeeded.remark()).isEmpty();
        } finally {
            publisher.stop();
        }
    }

    @Test
    void asyncCompletionPublishesExactlyOnce() throws Exception {
        PipkerLogProperties properties = properties();
        LinkedBlockingQueue<ApiAccessLogRecord> captured = new LinkedBlockingQueue<>();
        AsyncApiAccessLogPublisher publisher = new AsyncApiAccessLogPublisher(
                properties,
                List.of(records -> captured.addAll(records))
        );
        publisher.start();
        try {
            PipkerRequestLoggingFilter filter = filter(properties, publisher);
            MockHttpServletRequest request = request("/api/async");
            request.setAsyncSupported(true);
            MockHttpServletResponse response = response();
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
                servletRequest.startAsync();
                servletResponse.setContentType("application/json");
                servletResponse.getWriter().write("{\"code\":200}");
            });
            request.getAsyncContext().complete();

            assertThat(captured.poll(1, TimeUnit.SECONDS)).isNotNull();
            assertThat(captured.poll(120, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            publisher.stop();
        }
    }

    @Test
    void asyncErrorsAndTimeoutsEachPublishExactlyOneFailedRecord() throws Exception {
        PipkerLogProperties properties = properties();
        LinkedBlockingQueue<ApiAccessLogRecord> captured = new LinkedBlockingQueue<>();
        AsyncApiAccessLogPublisher publisher = new AsyncApiAccessLogPublisher(
                properties,
                List.of(records -> captured.addAll(records))
        );
        publisher.start();
        try {
            PipkerRequestLoggingFilter filter = filter(properties, publisher);

            MockHttpServletRequest errorRequest = startAsyncRequest(filter, "/api/async/error");
            MockAsyncContext errorContext = (MockAsyncContext) errorRequest.getAsyncContext();
            AsyncListener errorListener = errorContext.getListeners().getFirst();
            errorListener.onError(new AsyncEvent(errorContext, new IllegalStateException("async failed")));
            errorListener.onComplete(new AsyncEvent(errorContext));

            ApiAccessLogRecord error = captured.poll(1, TimeUnit.SECONDS);
            assertThat(error).isNotNull();
            assertThat(error.success()).isFalse();
            assertThat(error.exceptionType()).isEqualTo(IllegalStateException.class.getName());
            assertThat(captured.poll(120, TimeUnit.MILLISECONDS)).isNull();

            MockHttpServletRequest timeoutRequest = startAsyncRequest(filter, "/api/async/timeout");
            MockAsyncContext timeoutContext = (MockAsyncContext) timeoutRequest.getAsyncContext();
            AsyncListener timeoutListener = timeoutContext.getListeners().getFirst();
            timeoutListener.onTimeout(new AsyncEvent(timeoutContext));
            timeoutListener.onComplete(new AsyncEvent(timeoutContext));

            ApiAccessLogRecord timeout = captured.poll(1, TimeUnit.SECONDS);
            assertThat(timeout).isNotNull();
            assertThat(timeout.success()).isFalse();
            assertThat(timeout.exceptionType()).isEqualTo("jakarta.servlet.AsyncTimeout");
            assertThat(captured.poll(120, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            publisher.stop();
        }
    }

    @Test
    void boundedPublisherDropsNewRecordsAndIsolatesHandlerFailures() throws Exception {
        PipkerLogProperties properties = properties();
        properties.getApiAccess().setQueueCapacity(1);
        properties.getApiAccess().setBatchSize(1);
        properties.getApiAccess().setFlushInterval(Duration.ofMillis(5));
        properties.getApiAccess().setShutdownDrainTimeout(Duration.ofSeconds(1));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger handled = new AtomicInteger();
        AsyncApiAccessLogPublisher publisher = new AsyncApiAccessLogPublisher(properties, List.of(records -> {
            entered.countDown();
            try {
                release.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            handled.addAndGet(records.size());
        }));
        publisher.start();
        try {
            publisher.publish(record("/api/1"));
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            publisher.publish(record("/api/2"));
            publisher.publish(record("/api/3"));
            assertThat(publisher.snapshot().droppedSinceStartup()).isEqualTo(1);
            release.countDown();
            await(() -> handled.get() == 2);
        } finally {
            release.countDown();
            publisher.stop();
        }

        AsyncApiAccessLogPublisher failingPublisher = new AsyncApiAccessLogPublisher(
                properties,
                List.of(records -> { throw new IllegalStateException("database unavailable"); })
        );
        failingPublisher.start();
        try {
            failingPublisher.publish(record("/api/failure"));
            await(() -> failingPublisher.snapshot().writeFailuresSinceStartup() == 1);
            ApiAccessLogDiagnostics.Snapshot snapshot = failingPublisher.snapshot();
            assertThat(snapshot.writeFailuresSinceStartup()).isEqualTo(1);
        } finally {
            failingPublisher.stop();
        }
    }

    private ProceedingJoinPoint joinPoint(String methodName, Object result, Throwable failure) throws Throwable {
        Method method = TestController.class.getDeclaredMethod(methodName);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toLongString()).thenReturn("TestController." + methodName + "()");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        if (failure == null) {
            when(joinPoint.proceed()).thenReturn(result);
        } else {
            when(joinPoint.proceed()).thenThrow(failure);
        }
        return joinPoint;
    }

    private PipkerRequestLoggingFilter filter(
            PipkerLogProperties properties,
            AsyncApiAccessLogPublisher publisher
    ) {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        return new PipkerRequestLoggingFilter(
                properties,
                new StandardEnvironment(),
                new DefaultLogValueRenderer(new DefaultLogSanitizer(properties), jsonMapper),
                jsonMapper,
                List.of(),
                null,
                publisher
        );
    }

    private MockHttpServletRequest startAsyncRequest(
            PipkerRequestLoggingFilter filter,
            String path
    ) throws Exception {
        MockHttpServletRequest request = request(path);
        request.setAsyncSupported(true);
        filter.doFilter(request, response(), (servletRequest, servletResponse) -> servletRequest.startAsync());
        return request;
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("192.0.2.30");
        return request;
    }

    private MockHttpServletResponse response() {
        return new MockHttpServletResponse();
    }

    private PipkerLogProperties properties() {
        PipkerLogProperties properties = new PipkerLogProperties();
        properties.getContext().setServiceName("api-log-test");
        properties.getRequest().setEnabled(false);
        properties.getSlowRequest().setEnabled(false);
        return properties;
    }

    private ApiAccessLogRecord record(String path) {
        return new ApiAccessLogRecord(
                Instant.now(), "test", "GET", path, path, null, "", "",
                200, 200, true, 1L, 2L, null, null, null, null, null
        );
    }

    private void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static final class TestController {
        @ApiLog(purpose = " 查询接口用途 ", remark = " 补充说明 ")
        public String annotated() {
            return "ok";
        }

        public String unannotated() {
            return "plain";
        }
    }
}
