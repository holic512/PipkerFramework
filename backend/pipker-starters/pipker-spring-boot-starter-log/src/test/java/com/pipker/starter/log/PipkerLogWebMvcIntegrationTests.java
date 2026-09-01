package com.pipker.starter.log;

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
import org.springframework.web.bind.annotation.RestController;

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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ProbeController.class)
    static class TestApplication {
    }

    @RestController
    static class ProbeController {

        @GetMapping("/log/probe")
        String probe() {
            return MDC.get(LogMdcKeys.TRACE_ID);
        }
    }
}
