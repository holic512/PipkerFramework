package com.pipker.starter.log;

import com.pipker.starter.log.annotation.ApiLog;
import com.pipker.starter.log.api.ApiAccessLogHandler;
import com.pipker.starter.log.api.ApiAccessLogRecord;
import com.pipker.starter.log.context.LogMdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
                "pipker.log.request.enabled=false",
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

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    @Test
    void autoConfiguredFilterReusesTraceIdAndCleansMdcAfterMockMvcRequest() throws Exception {
        mockMvc.perform(get("/log/probe").header("X-Trace-Id", "upstream-integration-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "upstream-integration-1"))
                .andExpect(content().string("upstream-integration-1"));

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

        mockMvc.perform(get("/api/log/business-failure"))
                .andExpect(status().isOk());
        ApiAccessLogRecord businessFailure = apiAccessLogHandler.poll();
        assertThat(businessFailure).isNotNull();
        assertThat(businessFailure.businessCode()).isEqualTo(403);
        assertThat(businessFailure.success()).isFalse();

        mockMvc.perform(get("/api/log/required"))
                .andExpect(status().isBadRequest());
        ApiAccessLogRecord bindingFailure = apiAccessLogHandler.poll();
        assertThat(bindingFailure).isNotNull();
        assertThat(bindingFailure.routePattern()).isEqualTo("/api/log/required");
        assertThat(bindingFailure.success()).isFalse();

        mockMvc.perform(get("/api/log/missing"))
                .andExpect(status().isNotFound());
        ApiAccessLogRecord unmatched = apiAccessLogHandler.poll();
        assertThat(unmatched).isNotNull();
        assertThat(unmatched.requestPath()).isEqualTo("/api/log/missing");
        assertThat(unmatched.routePattern()).isNull();
        assertThat(unmatched.handlerMethod()).isNull();
        assertThat(unmatched.success()).isFalse();
        assertThat(apiAccessLogHandler.poll(120, TimeUnit.MILLISECONDS)).isNull();
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
