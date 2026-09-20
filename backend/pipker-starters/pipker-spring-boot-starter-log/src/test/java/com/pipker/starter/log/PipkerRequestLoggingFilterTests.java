package com.pipker.starter.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pipker.starter.log.api.ApiAccessLogRequestAttributes;
import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.context.LogMdcKeys;
import com.pipker.starter.log.sensitive.DefaultLogSanitizer;
import com.pipker.starter.log.sensitive.DefaultLogValueRenderer;
import com.pipker.starter.log.sensitive.LogValueRenderer;
import com.pipker.starter.log.sensitive.SensitiveType;
import com.pipker.starter.log.web.PipkerRequestLoggingFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void logsOneUnifiedOutcomeWithOnlySanitizedUrlQueryParameters() throws Exception {
        PipkerLogProperties properties = defaultProperties();
        properties.getRequest().setEnabled(true);
        properties.getRequest().setIncludeParameters(true);
        properties.getSensitive().getAdditionalFields().put("apiKey", SensitiveType.TOKEN);
        PipkerRequestLoggingFilter filter = createFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/42");
        request.setQueryString(
                "tag=first&tag=second&empty=&name=%E6%B5%8B%E8%AF%95"
                        + "&password=query-secret&apiKey=custom-secret"
        );
        request.setCharacterEncoding(StandardCharsets.UTF_8);
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent("formField=form-secret".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Bearer header-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        HandlerMethod handler = new HandlerMethod(
                new ProbeHandler(), ProbeHandler.class.getDeclaredMethod("handle")
        );

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, handler);
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/users/{id}");
            servletRequest.setAttribute(ApiAccessLogRequestAttributes.HANDLER_DURATION_MS, 7L);
            servletRequest.getInputStream().readAllBytes();
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write(
                    "{\"code\":403,\"accessToken\":\"response-secret\"}"
            );
        });

        List<String> httpLogs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("[HTTP]"))
                .toList();
        assertThat(httpLogs).hasSize(1);
        assertThat(httpLogs.getFirst())
                .contains(
                        "method=POST",
                        "path=/api/users/42",
                        "route=/api/users/{id}",
                        "httpStatus=200",
                        "businessCode=403",
                        "success=false",
                        "handlerDurationMs=7",
                        "\"tag\":[\"first\",\"second\"]",
                        "\"empty\":[\"\"]",
                        "\"name\":[\"测试\"]",
                        "\"password\":\"******\"",
                        "\"apiKey\":\"******\""
                )
                .doesNotContain(
                        "query-secret",
                        "custom-secret",
                        "form-secret",
                        "header-secret",
                        "response-secret",
                        "headers=",
                        "requestBody=",
                        "responseBody="
                );
    }

    @Test
    void logsSanitizedExceptionOutcomeAndRethrowsTheOriginalFailure() {
        PipkerLogProperties properties = defaultProperties();
        properties.getRequest().setEnabled(true);
        PipkerRequestLoggingFilter filter = createFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalStateException failure = new IllegalStateException(
                "password=plain-secret\nAuthorization=Bearer secret-token"
        );

        assertThatThrownBy(() -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw failure;
        })).isSameAs(failure);

        List<String> httpLogs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("[HTTP]"))
                .toList();
        assertThat(httpLogs).hasSize(1);
        assertThat(httpLogs.getFirst())
                .contains(
                        "success=false",
                        "exceptionType=java.lang.IllegalStateException",
                        "password=****** Authorization=******"
                )
                .doesNotContain("plain-secret", "secret-token", "\n");
    }

    @Test
    void asyncCompletionLogsExactlyOnce() throws Exception {
        PipkerLogProperties properties = defaultProperties();
        properties.getRequest().setEnabled(true);
        PipkerRequestLoggingFilter filter = createFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/async");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.startAsync();
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"code\":200}");
        });
        request.getAsyncContext().complete();

        assertThat(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("[HTTP]")))
                .singleElement()
                .asString()
                .contains("path=/api/async", "businessCode=200", "success=true");
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

    private static final class ProbeHandler {
        public void handle() {
        }
    }
}
