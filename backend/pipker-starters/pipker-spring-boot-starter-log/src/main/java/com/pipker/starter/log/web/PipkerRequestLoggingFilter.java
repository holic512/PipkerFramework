/**
 * @file PipkerRequestLoggingFilter.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 使用单一 Servlet Filter 管理 TraceId、请求 MDC、HTTP 日志与慢请求日志。
 * @logic 建立受控 MDC 作用域，安全包装可选 Body，按请求完成时间输出日志，并在异步完成回调中恢复快照。
 * @dependencies Spring Web、Jakarta Servlet、PipkerLogProperties、LogValueRenderer、SLF4J
 * @index_tags log、trace、mdc、http、slow-request
 * @author holic512
 */
package com.pipker.starter.log.web;

import com.pipker.starter.log.config.PipkerLogProperties;
import com.pipker.starter.log.context.LogContext;
import com.pipker.starter.log.context.LogContextContributor;
import com.pipker.starter.log.context.LogMdcKeys;
import com.pipker.starter.log.context.MdcContextScope;
import com.pipker.starter.log.exception.ExceptionLogReporter;
import com.pipker.starter.log.sensitive.LogValueRenderer;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 在单个 Servlet Filter 中建立请求上下文，并输出 HTTP、慢请求和异常日志。
 */
public class PipkerRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger("pipker.http");
    private static final int TRACE_ID_MAX_LENGTH = 128;

    private final PipkerLogProperties properties;
    private final Environment environment;
    private final LogValueRenderer valueRenderer;
    private final JsonMapper jsonMapper;
    private final List<LogContextContributor> contributors;
    private final ExceptionLogReporter exceptionLogReporter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 创建请求日志 Filter。
     *
     * @param properties 日志配置
     * @param environment Spring 环境
     * @param valueRenderer 日志值渲染器
     * @param jsonMapper JSON 映射器
     * @param contributors 请求日志上下文贡献者
     * @param exceptionLogReporter 异常报告器，可为空
     */
    public PipkerRequestLoggingFilter(
            PipkerLogProperties properties,
            Environment environment,
            LogValueRenderer valueRenderer,
            JsonMapper jsonMapper,
            List<LogContextContributor> contributors,
            @Nullable ExceptionLogReporter exceptionLogReporter
    ) {
        this.properties = properties;
        this.environment = environment;
        this.valueRenderer = valueRenderer;
        this.jsonMapper = jsonMapper;
        this.contributors = List.copyOf(contributors);
        this.exceptionLogReporter = exceptionLogReporter;
    }

    /**
     * 当 Trace、普通请求日志和慢请求日志均关闭时跳过请求。
     *
     * @param request 当前请求
     * @return 是否跳过 Filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.getTrace().isEnabled()
                && !properties.getRequest().isEnabled()
                && !properties.getSlowRequest().isEnabled();
    }

    /**
     * 异步分派阶段不重复进入 Filter，改由异步完成监听器记录请求。
     *
     * @return 始终返回 {@code true}
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    /**
     * 建立请求 MDC，按配置包装请求/响应，并在同步或异步完成时输出日志。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 后续 Filter 链
     * @throws ServletException Servlet 处理失败时抛出
     * @throws IOException 请求或响应 I/O 失败时抛出
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        Map<String, String> contextValues = createContextValues(request);
        LogContext context = new LogContext(contextValues);
        contribute(context);
        Map<String, String> mdcValues = context.snapshot();

        HttpServletRequest wrappedRequest = wrapRequestIfNecessary(request);
        HttpServletResponse wrappedResponse = wrapResponseIfNecessary(response);
        if (properties.getTrace().isEnabled() && properties.getTrace().isWriteResponseHeader()) {
            response.setHeader(properties.getTrace().getHeaderName(), mdcValues.get(LogMdcKeys.TRACE_ID));
        }

        try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
            } catch (ServletException | IOException | RuntimeException | Error exception) {
                reportException(request, exception);
                throw exception;
            } finally {
                if (request.isAsyncStarted()) {
                    registerAsyncCompletion(request, wrappedRequest, wrappedResponse, mdcValues, startedAt);
                } else {
                    logCompletedRequest(wrappedRequest, wrappedResponse, startedAt);
                }
            }
        }
    }

    /**
     * 创建由 Filter 管理的基础日志上下文。
     */
    private Map<String, String> createContextValues(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        if (properties.getTrace().isEnabled()) {
            values.put(LogMdcKeys.TRACE_ID, resolveTraceId(request));
        }
        if (properties.getContext().isRequestIdEnabled()) {
            values.put(LogMdcKeys.REQUEST_ID, randomId());
        }
        values.put(LogMdcKeys.SERVICE_NAME, resolveServiceName());
        if (properties.getContext().isClientIpEnabled()) {
            values.put(LogMdcKeys.CLIENT_IP, request.getRemoteAddr());
        }
        values.put(LogMdcKeys.HTTP_METHOD, request.getMethod());
        values.put(LogMdcKeys.REQUEST_URI, request.getRequestURI());
        return values;
    }

    /**
     * 依次执行上下文贡献者，并隔离单个贡献者的运行时异常。
     */
    private void contribute(LogContext context) {
        for (LogContextContributor contributor : contributors) {
            try {
                contributor.contribute(context);
            } catch (RuntimeException exception) {
                LOGGER.debug("Log context contributor failed and was ignored", exception);
            }
        }
    }

    /**
     * 仅在启用请求体日志且媒体类型允许时包装请求。
     */
    private HttpServletRequest wrapRequestIfNecessary(HttpServletRequest request) {
        if (!properties.getRequest().isEnabled()
                || !properties.getRequest().isIncludeRequestBody()
                || !isBodyContentAllowed(request.getContentType())) {
            return request;
        }
        if (request instanceof ContentCachingRequestWrapper) {
            return request;
        }
        return new ContentCachingRequestWrapper(request, bodyLimit());
    }

    /**
     * 仅在启用响应体日志时使用受限响应捕获包装器。
     */
    private HttpServletResponse wrapResponseIfNecessary(HttpServletResponse response) {
        if (!properties.getRequest().isEnabled() || !properties.getRequest().isIncludeResponseBody()) {
            return response;
        }
        return new BoundedResponseCaptureWrapper(response, bodyLimit());
    }

    /**
     * 为异步请求注册完成、超时、错误和再次启动回调。
     */
    private void registerAsyncCompletion(
            HttpServletRequest request,
            HttpServletRequest wrappedRequest,
            HttpServletResponse wrappedResponse,
            Map<String, String> mdcValues,
            long startedAt
    ) {
        try {
            request.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
                        logCompletedRequest(wrappedRequest, wrappedResponse, startedAt);
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    logAsyncFailure(request, event.getThrowable());
                }

                @Override
                public void onError(AsyncEvent event) {
                    logAsyncFailure(request, event.getThrowable());
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                    event.getAsyncContext().addListener(this);
                }
            });
        } catch (IllegalStateException exception) {
            LOGGER.debug("Could not register async request logging callback", exception);
        }
    }

    /**
     * 报告异步请求阶段收到的异常。
     */
    private void logAsyncFailure(HttpServletRequest request, Throwable exception) {
        if (exception != null) {
            reportException(request, exception);
        }
    }

    /**
     * 在请求完成时按忽略规则输出普通请求日志和慢请求日志。
     */
    private void logCompletedRequest(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        if (isIgnoredPath(request.getRequestURI())) {
            return;
        }
        long costMillis = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        if (properties.getRequest().isEnabled()) {
            logRequest(request, response, costMillis);
        }
        if (properties.getSlowRequest().isEnabled() && costMillis >= Math.max(0, properties.getSlowRequest().getThreshold())) {
            logAt(
                    properties.getSlowRequest().getLevel(),
                    "[SLOW] {} {} status={} cost={}ms clientIp={} traceId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), costMillis,
                    MDC.get(LogMdcKeys.CLIENT_IP), MDC.get(LogMdcKeys.TRACE_ID)
            );
        }
    }

    /**
     * 组装并输出普通 HTTP 请求日志。
     */
    private void logRequest(HttpServletRequest request, HttpServletResponse response, long costMillis) {
        String parameters = properties.getRequest().isIncludeParameters() ? renderParameters(request) : null;
        String headers = properties.getRequest().isIncludeHeaders() ? renderHeaders(request) : null;
        String requestBody = properties.getRequest().isIncludeRequestBody() ? renderRequestBody(request) : null;
        String responseBody = properties.getRequest().isIncludeResponseBody() ? renderResponseBody(response) : null;
        logAt(
                properties.getRequest().getLevel(),
                "[HTTP] {} {} status={} cost={}ms clientIp={} traceId={} params={} headers={} requestBody={} responseBody={}",
                request.getMethod(), request.getRequestURI(), response.getStatus(), costMillis,
                MDC.get(LogMdcKeys.CLIENT_IP), MDC.get(LogMdcKeys.TRACE_ID),
                parameters, headers, requestBody, responseBody
        );
    }

    /**
     * 将请求参数渲染为脱敏 JSON 文本。
     */
    private String renderParameters(HttpServletRequest request) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> parameters.put(name, List.of(values)));
        return valueRenderer.render(parameters, properties.getOperation().getMaxValueLength());
    }

    /**
     * 将请求头渲染为脱敏 JSON 文本。
     */
    private String renderHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            List<String> values = new ArrayList<>();
            Enumeration<String> headerValues = request.getHeaders(name);
            while (headerValues.hasMoreElements()) {
                values.add(headerValues.nextElement());
            }
            headers.put(name, values);
        }
        return valueRenderer.render(headers, properties.getOperation().getMaxValueLength());
    }

    /**
     * 读取已缓存且媒体类型允许的请求体并渲染为 JSON 文本。
     */
    private String renderRequestBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper) || !isBodyContentAllowed(request.getContentType())) {
            return null;
        }
        return renderJsonBody(wrapper.getContentAsByteArray(), false);
    }

    /**
     * 读取已捕获且媒体类型允许的响应体并渲染为 JSON 文本。
     */
    private String renderResponseBody(HttpServletResponse response) {
        if (!(response instanceof BoundedResponseCaptureWrapper wrapper) || !isBodyContentAllowed(response.getContentType())) {
            return null;
        }
        String rendered = renderJsonBody(wrapper.getCapturedContent(), wrapper.isTruncated());
        return rendered;
    }

    /**
     * 解析 JSON 请求体；空内容或无效 JSON 使用空值或安全占位值表示。
     */
    private String renderJsonBody(byte[] body, boolean truncated) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            Object parsed = jsonMapper.readValue(body, Object.class);
            String rendered = valueRenderer.render(parsed, bodyLimit());
            return truncated ? rendered + "…" : rendered;
        } catch (RuntimeException exception) {
            return "<unavailable: invalid-json>";
        }
    }

    /**
     * 判断媒体类型是否允许读取并解析内容体。
     */
    private boolean isBodyContentAllowed(String contentType) {
        if (contentType == null || contentType.isBlank() || matchesContentType(contentType, properties.getRequest().getIgnoredContentTypes())) {
            return false;
        }
        return matchesContentType(contentType, properties.getRequest().getBodyContentTypes());
    }

    /**
     * 使用大小写不敏感的通配模式匹配媒体类型。
     */
    private boolean matchesContentType(String contentType, List<String> patterns) {
        if (contentType == null || patterns == null) {
            return false;
        }
        String actual = contentType.toLowerCase(java.util.Locale.ROOT).split(";", 2)[0].trim();
        return patterns.stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .map(pattern -> pattern.toLowerCase(java.util.Locale.ROOT).trim())
                .anyMatch(pattern -> contentTypeMatches(actual, pattern));
    }

    /**
     * 执行单个媒体类型模式的精确或前后缀匹配。
     */
    private boolean contentTypeMatches(String actual, String pattern) {
        if (pattern.equals(actual)) {
            return true;
        }
        if (pattern.startsWith("*") && pattern.endsWith("*")) {
            return actual.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return actual.endsWith(pattern.substring(1));
        }
        if (pattern.endsWith("*")) {
            return actual.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return false;
    }

    /**
     * 判断请求路径是否命中忽略路径模式。
     */
    private boolean isIgnoredPath(String path) {
        List<String> ignoredPaths = properties.getRequest().getIgnoredPaths();
        return ignoredPaths != null && ignoredPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 优先采用合法的上游 TraceId，否则生成新的随机标识。
     */
    private String resolveTraceId(HttpServletRequest request) {
        if (properties.getTrace().isAcceptUpstream()) {
            String upstream = request.getHeader(properties.getTrace().getHeaderName());
            if (isValidTraceId(upstream)) {
                return upstream;
            }
        }
        return randomId();
    }

    /**
     * 校验 TraceId 的长度和允许字符，避免将任意请求头写入 MDC。
     */
    private boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.isBlank() || traceId.length() > TRACE_ID_MAX_LENGTH) {
            return false;
        }
        return traceId.chars().allMatch(character -> Character.isLetterOrDigit(character)
                || character == '-'
                || character == '_'
                || character == '.');
    }

    /**
     * 按配置、应用名和固定默认值的顺序解析服务名称。
     */
    private String resolveServiceName() {
        String configured = properties.getContext().getServiceName();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String applicationName = environment.getProperty("spring.application.name");
        return applicationName == null || applicationName.isBlank() ? "application" : applicationName;
    }

    /**
     * 旁路报告异常，并保证报告器故障不改变原始请求结果。
     */
    private void reportException(HttpServletRequest request, Throwable exception) {
        if (exceptionLogReporter != null) {
            try {
                exceptionLogReporter.report(request, exception);
            } catch (RuntimeException ignored) {
                // 异常报告失败不能改变原异常传播。
            }
        }
    }

    /**
     * 返回至少为 1 的内容体捕获上限。
     */
    private int bodyLimit() {
        return Math.max(1, properties.getRequest().getMaxBodyLength());
    }

    /**
     * 生成不含连字符的随机请求标识。
     */
    private String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 将统一日志调用分派到实际日志等级。
     */
    private void logAt(LogLevel level, String message, Object... arguments) {
        LogLevel actualLevel = level == null ? LogLevel.INFO : level;
        switch (actualLevel) {
            case TRACE -> LOGGER.trace(message, arguments);
            case DEBUG -> LOGGER.debug(message, arguments);
            case INFO -> LOGGER.info(message, arguments);
            case WARN -> LOGGER.warn(message, arguments);
            case ERROR, FATAL -> LOGGER.error(message, arguments);
            case OFF -> {
            }
        }
    }
}
