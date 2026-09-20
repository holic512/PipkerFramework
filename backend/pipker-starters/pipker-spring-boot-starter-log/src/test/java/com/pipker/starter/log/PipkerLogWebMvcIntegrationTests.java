package com.pipker.starter.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pipker.starter.log.annotation.ApiLog;
import com.pipker.starter.log.api.ApiAccessLogHandler;
import com.pipker.starter.log.api.ApiAccessLogRecord;
import com.pipker.starter.log.context.LogMdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = PipkerLogWebMvcIntegrationTests.TestApplication.class,
        properties = {
                "pipker.log.request.enabled=true",
                "pipker.log.slow-request.enabled=false",
                "pipker.log.context.service-name=log-integration-test"
        }
)
@AutoConfigureMockMvc
class PipkerLogWebMvcIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingApiAccessLogHandler apiAccessLogHandler;

    private Logger httpLogger;
    private ListAppender<ILoggingEvent> httpAppender;

    @BeforeEach
    void captureHttpCompletionLogs() {
        httpLogger = (Logger) LoggerFactory.getLogger("pipker.http");
        httpAppender = new ListAppender<>();
        httpAppender.start();
        httpLogger.addAppender(httpAppender);
    }

    @AfterEach
    void cleanMdc() {
        httpLogger.detachAppender(httpAppender);
        httpAppender.stop();
        MDC.clear();
    }

    @Test
    void autoConfiguredFilterReusesTraceIdAndCleansMdcAfterMockMvcRequest() throws Exception {
        mockMvc.perform(get("/log/probe").header("X-Trace-Id", "upstream-integration-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "upstream-integration-1"))
                .andExpect(content().string("upstream-integration-1"));

        assertSingleHttpLogContains(
                "method=GET",
                "path=/log/probe",
                "httpStatus=200",
                "success=true",
                "traceId=upstream-integration-1"
        );
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void autoConfiguredApiPipelineCapturesMvcFailuresAndUnmatchedPaths() throws Exception {
        mockMvc.perform(get("/api/log/probe").header("X-Trace-Id", "api-success-trace"))
                .andExpect(status().isOk());
        ApiAccessLogRecord success = apiAccessLogHandler.poll();
        assertThat(success).isNotNull();
        assertThat(success.routePattern()).isEqualTo("/api/log/probe");
        assertThat(success.purpose()).isEqualTo("探测接口访问日志");
        assertThat(success.businessCode()).isEqualTo(200);
        assertThat(success.success()).isTrue();
        assertSingleHttpLogContains(
                "method=GET",
                "path=/api/log/probe",
                "route=/api/log/probe",
                "httpStatus=200",
                "businessCode=200",
                "success=true",
                "handlerDurationMs=",
                "durationMs=",
                "clientIp=127.0.0.1",
                "traceId=api-success-trace"
        );

        mockMvc.perform(get("/api/log/business-failure"))
                .andExpect(status().isOk());
        ApiAccessLogRecord businessFailure = apiAccessLogHandler.poll();
        assertThat(businessFailure).isNotNull();
        assertThat(businessFailure.businessCode()).isEqualTo(403);
        assertThat(businessFailure.success()).isFalse();
        assertSingleHttpLogContains(
                "path=/api/log/business-failure",
                "route=/api/log/business-failure",
                "httpStatus=200",
                "businessCode=403",
                "success=false"
        );

        mockMvc.perform(get("/api/log/required"))
                .andExpect(status().isBadRequest());
        ApiAccessLogRecord bindingFailure = apiAccessLogHandler.poll();
        assertThat(bindingFailure).isNotNull();
        assertThat(bindingFailure.routePattern()).isEqualTo("/api/log/required");
        assertThat(bindingFailure.success()).isFalse();
        assertSingleHttpLogContains(
                "path=/api/log/required",
                "route=/api/log/required",
                "httpStatus=400",
                "success=false"
        );

        mockMvc.perform(get("/api/log/missing"))
                .andExpect(status().isNotFound());
        ApiAccessLogRecord unmatched = apiAccessLogHandler.poll();
        assertThat(unmatched).isNotNull();
        assertThat(unmatched.requestPath()).isEqualTo("/api/log/missing");
        assertThat(unmatched.routePattern()).isNull();
        assertThat(unmatched.handlerMethod()).isNull();
        assertThat(unmatched.success()).isFalse();
        assertSingleHttpLogContains(
                "path=/api/log/missing",
                "route=null",
                "httpStatus=404",
                "success=false"
        );
        assertThat(apiAccessLogHandler.poll(120, TimeUnit.MILLISECONDS)).isNull();
    }

    private void assertSingleHttpLogContains(String... expectedFragments) {
        List<String> messages = httpAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("[HTTP]"))
                .toList();
        assertThat(messages).singleElement().asString().contains(expectedFragments);
        httpAppender.list.clear();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ProbeController.class, CapturingApiAccessLogHandler.class})
    static class TestApplication {
    }

    @RestController
    static class ProbeController {

        @GetMapping("/log/probe")
        String probe() {
            return MDC.get(LogMdcKeys.TRACE_ID);
        }

        @GetMapping("/api/log/probe")
        @ApiLog(purpose = "探测接口访问日志")
        public Map<String, Object> apiProbe() {
            return Map.of("code", 200, "data", "ok", "message", "OK");
        }

        @GetMapping("/api/log/business-failure")
        @ApiLog(purpose = "返回业务失败")
        public Map<String, Object> businessFailure() {
            return Map.of("code", 403, "data", Map.of(), "message", "Forbidden");
        }

        @GetMapping("/api/log/required")
        @ApiLog(purpose = "校验必填参数")
        public Map<String, Object> required(@RequestParam int value) {
            return Map.of("code", 200, "data", value, "message", "OK");
        }
    }

    static class CapturingApiAccessLogHandler implements ApiAccessLogHandler {

        private final LinkedBlockingQueue<ApiAccessLogRecord> records = new LinkedBlockingQueue<>();

        @Override
        public void handle(List<ApiAccessLogRecord> batch) {
            records.addAll(batch);
        }

        ApiAccessLogRecord poll() throws InterruptedException {
            return poll(2, TimeUnit.SECONDS);
        }

        ApiAccessLogRecord poll(long timeout, TimeUnit unit) throws InterruptedException {
            return records.poll(timeout, unit);
        }
    }
}
