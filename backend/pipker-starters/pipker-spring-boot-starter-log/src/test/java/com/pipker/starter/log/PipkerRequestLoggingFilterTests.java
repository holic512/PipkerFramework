package com.pipker.starter.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.context.LogMdcKeys;
import com.pipker.starter.log.sensitive.DefaultLogSanitizer;
import com.pipker.starter.log.sensitive.DefaultLogValueRenderer;
import com.pipker.starter.log.sensitive.LogValueRenderer;
import com.pipker.starter.log.web.PipkerRequestLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PipkerRequestLoggingFilterTests {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private boolean previousAdditivity;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger("pipker.http");
        logger.setLevel(Level.TRACE);
        previousAdditivity = logger.isAdditive();
        logger.setAdditive(false);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setAdditive(previousAdditivity);
        MDC.clear();
    }

    @Test
    void reusesUpstreamTraceIdReturnsItAndCleansMdc() throws Exception {
        PipkerLogProperties properties = defaultProperties();
        PipkerRequestLoggingFilter filter = createFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/probe");
        request.addHeader("X-Trace-Id", "upstream-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(MDC.get(LogMdcKeys.TRACE_ID)).isEqualTo("upstream-123");
            assertThat(MDC.get(LogMdcKeys.REQUEST_ID)).isNotBlank();
            servletResponse.getWriter().write("ok");
        });

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("upstream-123");
        assertThat(MDC.getCopyOfContextMap()).isNull();
        assertThat(appender.list)
                .noneMatch(event -> event.getFormattedMessage().contains("[HTTP]"));
    }

    @Test
    void generatesTraceIdWhenTheRequestDoesNotProvideOne() throws Exception {
        PipkerRequestLoggingFilter filter = createFilter(defaultProperties());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/generated-trace");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                servletResponse.getWriter().write(MDC.get(LogMdcKeys.TRACE_ID)));

        assertThat(response.getHeader("X-Trace-Id")).matches("[a-f0-9]{32}");
        assertThat(response.getContentAsString()).isEqualTo(response.getHeader("X-Trace-Id"));
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    @Test
    void logsSanitizedBoundedJsonBodiesAndSlowRequestWithoutChangingResponse() throws Exception {
        PipkerLogProperties properties = defaultProperties();
        properties.getRequest().setEnabled(true);
        properties.getRequest().setIncludeRequestBody(true);
        properties.getRequest().setIncludeResponseBody(true);
        properties.getRequest().setMaxBodyLength(128);
        properties.getSlowRequest().setEnabled(true);
        properties.getSlowRequest().setThreshold(0);
        PipkerRequestLoggingFilter filter = createFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContentType("application/json");
        request.setContent("{\"password\":\"plain-secret\",\"name\":\"Pipker\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.getInputStream().readAllBytes();
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"accessToken\":\"token-value\",\"message\":\"ok\"}");
        });

        assertThat(response.getContentAsString()).contains("token-value").contains("message");
        assertThat(appender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("[HTTP]")
                        && event.getFormattedMessage().contains("******")
                        && !event.getFormattedMessage().contains("plain-secret")
                        && !event.getFormattedMessage().contains("token-value"));
        assertThat(appender.list).anyMatch(event -> event.getFormattedMessage().contains("[SLOW]"));
    }

    private PipkerRequestLoggingFilter createFilter(PipkerLogProperties properties) {
        LogValueRenderer renderer = new DefaultLogValueRenderer(new DefaultLogSanitizer(properties), JsonMapper.builder().build());
        StandardEnvironment environment = new StandardEnvironment();
        return new PipkerRequestLoggingFilter(
                properties,
                environment,
                renderer,
                JsonMapper.builder().build(),
                List.of(),
                null
        );
    }

    private PipkerLogProperties defaultProperties() {
        PipkerLogProperties properties = new PipkerLogProperties();
        properties.getContext().setServiceName("log-test");
        properties.getRequest().setEnabled(false);
        properties.getSlowRequest().setEnabled(false);
        return properties;
    }
}
