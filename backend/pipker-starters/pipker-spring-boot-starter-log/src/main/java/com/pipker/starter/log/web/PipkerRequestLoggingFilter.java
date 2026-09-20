/**
 * @file PipkerRequestLoggingFilter.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 使用单一 Servlet Filter 管理 TraceId、请求 MDC、统一 HTTP 完成日志、慢请求日志与接口访问采集。
 * @logic 建立受控 MDC 作用域并有界包装响应；同步或异步完成时只生成一次安全快照，共享给控制台摘要与数据库异步发布。
 * @dependencies Pipker Common Starter、Spring Web、Jakarta Servlet、PipkerLogProperties、ApiAccessLogPublisher、LogValueRenderer、SLF4J
 * @index_tags log、trace、mdc、http、slow-request、api-access、async
 * @author holic512
 */
package com.pipker.starter.log.web;

import com.pipker.starter.common.util.UuidUtils;
import com.pipker.starter.log.api.ApiAccessLogRecord;
import com.pipker.starter.log.api.ApiAccessLogMessageSanitizer;
import com.pipker.starter.log.api.ApiAccessLogRequestAttributes;
import com.pipker.starter.log.api.AsyncApiAccessLogPublisher;
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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AsyncApiAccessLogPublisher apiAccessLogPublisher;
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
        this(properties, environment, valueRenderer, jsonMapper, contributors, exceptionLogReporter, null);
    }

    /** 创建带接口访问日志异步发布能力的请求 Filter。 */
    public PipkerRequestLoggingFilter(
            PipkerLogProperties properties,
            Environment environment,
            LogValueRenderer valueRenderer,
            JsonMapper jsonMapper,
            List<LogContextContributor> contributors,
            @Nullable ExceptionLogReporter exceptionLogReporter,
            @Nullable AsyncApiAccessLogPublisher apiAccessLogPublisher
    ) {
        this.properties = properties;
        this.environment = environment;
        this.valueRenderer = valueRenderer;
        this.jsonMapper = jsonMapper;
        this.contributors = List.copyOf(contributors);
        this.exceptionLogReporter = exceptionLogReporter;
        this.apiAccessLogPublisher = apiAccessLogPublisher;
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
                && !properties.getSlowRequest().isEnabled()
                && !properties.getApiAccess().isEnabled();
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
        Instant occurredAt = Instant.now();
        AtomicBoolean completed = new AtomicBoolean();
        Map<String, String> contextValues = createContextValues(request);
        LogContext context = new LogContext(contextValues);
        contribute(context);
        Map<String, String> mdcValues = context.snapshot();

        HttpServletRequest wrappedRequest = wrapRequestIfNecessary(request);
        HttpServletResponse wrappedResponse = wrapResponseIfNecessary(request, response);
        if (properties.getTrace().isEnabled() && properties.getTrace().isWriteResponseHeader()) {
            response.setHeader(properties.getTrace().getHeaderName(), mdcValues.get(LogMdcKeys.TRACE_ID));
        }

        try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
            try {
                filterChain.doFilter(wrappedRequest, wrappedResponse);
            } catch (ServletException | IOException | RuntimeException | Error exception) {
                request.setAttribute(ApiAccessLogRequestAttributes.EXCEPTION_TYPE, exception.getClass().getName());
                request.setAttribute(ApiAccessLogRequestAttributes.EXCEPTION_MESSAGE,
                        sanitizeExceptionMessage(exception.getMessage()));
                reportException(request, exception);
                throw exception;
            } finally {
                if (request.isAsyncStarted()) {
                    registerAsyncCompletion(
                            request, wrappedRequest, wrappedResponse, mdcValues, occurredAt, startedAt, completed
                    );
                } else {
                    completeRequest(wrappedRequest, wrappedResponse, occurredAt, startedAt, completed);
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
            values.put(LogMdcKeys.REQUEST_ID, UuidUtils.randomUuidWithoutHyphens());
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

    /** 仅在需要响应体日志或 API 结果快照时使用受限响应捕获包装器。 */
    private HttpServletResponse wrapResponseIfNecessary(HttpServletRequest request, HttpServletResponse response) {
        boolean responseBodyLogging = properties.getRequest().isEnabled()
                && properties.getRequest().isIncludeResponseBody();
        boolean outcomeCapture = isApiPath(request.getRequestURI())
                && (properties.getRequest().isEnabled() || properties.getApiAccess().isEnabled());
        if (!responseBodyLogging && !outcomeCapture) {
            return response;
        }
        int maxLength = responseBodyLogging
                ? Math.max(bodyLimit(), responsePrefixLimit())
                : responsePrefixLimit();
        return new BoundedResponseCaptureWrapper(response, maxLength);
    }

    /**
     * 为异步请求注册完成、超时、错误和再次启动回调。
     */
    private void registerAsyncCompletion(
            HttpServletRequest request,
            HttpServletRequest wrappedRequest,
            HttpServletResponse wrappedResponse,
            Map<String, String> mdcValues,
            Instant occurredAt,
            long startedAt,
            AtomicBoolean completed
    ) {
        try {
            request.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
                        completeRequest(wrappedRequest, wrappedResponse, occurredAt, startedAt, completed);
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    markAsyncFailure(request, event.getThrowable(), "jakarta.servlet.AsyncTimeout");
                    try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
                        logAsyncFailure(request, event.getThrowable());
                        completeRequest(wrappedRequest, wrappedResponse, occurredAt, startedAt, completed);
                    }
                }

                @Override
                public void onError(AsyncEvent event) {
                    markAsyncFailure(request, event.getThrowable(), "jakarta.servlet.AsyncError");
                    try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
                        logAsyncFailure(request, event.getThrowable());
                        completeRequest(wrappedRequest, wrappedResponse, occurredAt, startedAt, completed);
                    }
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                    event.getAsyncContext().addListener(this);
                }
            });
        } catch (IllegalStateException exception) {
            LOGGER.debug("Could not register async request logging callback", exception);
            try (MdcContextScope ignored = MdcContextScope.activate(mdcValues)) {
                completeRequest(wrappedRequest, wrappedResponse, occurredAt, startedAt, completed);
            }
        }
    }

    /** 确保同步、异步和重复回调只完成一次访问日志。 */
    private void completeRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            Instant occurredAt,
            long startedAt,
            AtomicBoolean completed
    ) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        logCompletedRequest(request, response, occurredAt, startedAt);
    }

    /**
     * 报告异步请求阶段收到的异常。
     */
    private void logAsyncFailure(HttpServletRequest request, Throwable exception) {
        if (exception != null) {
            reportException(request, exception);
        }
    }

    private void markAsyncFailure(HttpServletRequest request, Throwable exception, String fallbackType) {
        request.setAttribute(ApiAccessLogRequestAttributes.ASYNC_FAILURE, true);
        request.setAttribute(ApiAccessLogRequestAttributes.EXCEPTION_TYPE,
                exception == null ? fallbackType : exception.getClass().getName());
        if (exception != null) {
            request.setAttribute(ApiAccessLogRequestAttributes.EXCEPTION_MESSAGE,
                    sanitizeExceptionMessage(exception.getMessage()));
        }
    }

    /** 在请求完成时生成一次快照，再输出 HTTP/慢请求日志并发布数据库记录。 */
    private void logCompletedRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            Instant occurredAt,
            long startedAt
    ) {
        long costMillis = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        RequestCompletionSnapshot snapshot = createCompletionSnapshot(request, response, costMillis);
        if (!isIgnoredPath(request.getRequestURI())) {
            if (properties.getRequest().isEnabled()) {
                logRequest(request, response, snapshot);
            }
            if (properties.getSlowRequest().isEnabled()
                    && costMillis >= Math.max(0, properties.getSlowRequest().getThreshold())) {
                logAt(
                        properties.getSlowRequest().getLevel(),
                        "[SLOW] {} {} status={} cost={}ms clientIp={} traceId={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), costMillis,
                        MDC.get(LogMdcKeys.CLIENT_IP), MDC.get(LogMdcKeys.TRACE_ID)
                );
            }
        }
        publishApiAccessLog(request, occurredAt, snapshot);
    }

    /** 从当前请求和有界响应前缀生成控制台与数据库共享的完成快照。 */
    private RequestCompletionSnapshot createCompletionSnapshot(
            HttpServletRequest request,
            HttpServletResponse response,
            long durationMs
    ) {
        Integer businessCode = extractBusinessCode(response);
        String exceptionType = truncate(
                attributeString(request, ApiAccessLogRequestAttributes.EXCEPTION_TYPE), 255
        );
        String exceptionMessage = truncate(
                attributeString(request, ApiAccessLogRequestAttributes.EXCEPTION_MESSAGE), 1_024
        );
        boolean asyncFailure = Boolean.TRUE.equals(request.getAttribute(ApiAccessLogRequestAttributes.ASYNC_FAILURE));
        boolean httpSuccess = response.getStatus() >= 200 && response.getStatus() < 300;
        boolean success = !asyncFailure && exceptionType == null && httpSuccess
                && (businessCode == null || businessCode == 200);
        Object handlerDuration = request.getAttribute(ApiAccessLogRequestAttributes.HANDLER_DURATION_MS);
        Object bestMatchingHandler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        Object routePattern = bestMatchingHandler instanceof HandlerMethod
                ? request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                : null;
        return new RequestCompletionSnapshot(
                truncate(routePattern == null ? null : routePattern.toString(), 512),
                response.getStatus(),
                businessCode,
                success,
                handlerDuration instanceof Number number ? Math.max(0, number.longValue()) : null,
                durationMs,
                exceptionType,
                exceptionMessage
        );
    }

    /** 构造不含请求或响应内容的接口访问记录并非阻塞发布。 */
    private void publishApiAccessLog(
            HttpServletRequest request,
            Instant occurredAt,
            RequestCompletionSnapshot snapshot
    ) {
        if (apiAccessLogPublisher == null || !isApiAccessIncluded(request.getRequestURI())) {
            return;
        }
        apiAccessLogPublisher.publish(new ApiAccessLogRecord(
                occurredAt,
                truncate(resolveServiceName(), 100),
                truncate(request.getMethod(), 16),
                truncate(request.getRequestURI(), 512),
                snapshot.routePattern(),
                truncate(attributeString(request, ApiAccessLogRequestAttributes.HANDLER_METHOD), 512),
                defaultString(truncate(attributeString(request, ApiAccessLogRequestAttributes.PURPOSE), 255)),
                defaultString(truncate(attributeString(request, ApiAccessLogRequestAttributes.REMARK), 500)),
                snapshot.httpStatus(),
                snapshot.businessCode(),
                snapshot.success(),
                snapshot.handlerDurationMs(),
                snapshot.durationMs(),
                truncate(MDC.get(LogMdcKeys.TRACE_ID), 128),
                truncate(MDC.get(LogMdcKeys.REQUEST_ID), 64),
                truncate(MDC.get(LogMdcKeys.CLIENT_IP), 64),
                snapshot.exceptionType(),
                snapshot.exceptionMessage()
        ));
    }

    /** 组装并输出一条固定字段的 HTTP 完成日志，按显式开关附加内容字段。 */
    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            RequestCompletionSnapshot snapshot
    ) {
        String parameters = properties.getRequest().isIncludeParameters() ? renderParameters(request) : null;
        StringBuilder message = new StringBuilder(
                "[HTTP] method={} path={} route={} httpStatus={} businessCode={} success={} "
                        + "handlerDurationMs={} durationMs={} clientIp={} traceId={} params={} "
                        + "exceptionType={} exceptionMessage={}"
        );
        List<Object> arguments = new ArrayList<>();
        arguments.add(request.getMethod());
        arguments.add(request.getRequestURI());
        arguments.add(snapshot.routePattern());
        arguments.add(snapshot.httpStatus());
        arguments.add(snapshot.businessCode());
        arguments.add(snapshot.success());
        arguments.add(snapshot.handlerDurationMs());
        arguments.add(snapshot.durationMs());
        arguments.add(MDC.get(LogMdcKeys.CLIENT_IP));
        arguments.add(MDC.get(LogMdcKeys.TRACE_ID));
        arguments.add(parameters);
        arguments.add(snapshot.exceptionType());
        arguments.add(snapshot.exceptionMessage());
        if (properties.getRequest().isIncludeHeaders()) {
            message.append(" headers={}");
            arguments.add(renderHeaders(request));
        }
        if (properties.getRequest().isIncludeRequestBody()) {
            message.append(" requestBody={}");
            arguments.add(renderRequestBody(request));
        }
        if (properties.getRequest().isIncludeResponseBody()) {
            message.append(" responseBody={}");
            arguments.add(renderResponseBody(response));
        }
        logAt(
                properties.getRequest().getLevel(),
                message.toString(),
                arguments.toArray()
        );
    }

    /** 独立解析 URL 查询串并渲染为脱敏 JSON，不读取表单或其他请求体。 */
    private String renderParameters(HttpServletRequest request) {
        String query = request.getQueryString();
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) {
            return valueRenderer.render(parameters, properties.getOperation().getMaxValueLength());
        }
        Charset charset = resolveCharset(request.getCharacterEncoding());
        try {
            for (String pair : query.split("&", -1)) {
                if (pair.isEmpty()) {
                    continue;
                }
                int separator = pair.indexOf('=');
                String rawName = separator < 0 ? pair : pair.substring(0, separator);
                String rawValue = separator < 0 ? "" : pair.substring(separator + 1);
                String name = URLDecoder.decode(rawName, charset);
                String value = URLDecoder.decode(rawValue, charset);
                parameters.computeIfAbsent(name, ignored -> new ArrayList<>()).add(value);
            }
        } catch (IllegalArgumentException exception) {
            parameters.clear();
            parameters.put("_query", List.of("<unavailable: invalid-encoding>"));
        }
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

    private boolean isApiAccessIncluded(String path) {
        return properties.getApiAccess().isEnabled() && isApiPath(path);
    }

    private boolean isApiPath(String path) {
        if (path == null) {
            return false;
        }
        List<String> patterns = properties.getApiAccess().getIncludePaths();
        return patterns != null && patterns.stream()
                .filter(pattern -> pattern != null && !pattern.isBlank())
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    private Integer extractBusinessCode(HttpServletResponse response) {
        if (!(response instanceof BoundedResponseCaptureWrapper wrapper)) {
            return null;
        }
        byte[] content = wrapper.getCapturedContent();
        if (content.length == 0) {
            return null;
        }
        Charset charset = resolveCharset(response.getCharacterEncoding());
        return extractTopLevelBusinessCode(new String(content, charset));
    }

    /** 在有界 JSON 前缀中流式查找顶层数值 code，忽略嵌套对象中的同名字段。 */
    private Integer extractTopLevelBusinessCode(String json) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int stringStart = -1;
        for (int index = 0; index < json.length(); index++) {
            char character = json.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                    if (depth == 1 && "code".contentEquals(json.subSequence(stringStart, index))) {
                        Integer code = parseNumericFieldValue(json, index + 1);
                        if (code != null) {
                            return code;
                        }
                    }
                }
                continue;
            }
            if (character == '"') {
                inString = true;
                stringStart = index + 1;
            } else if (character == '{' || character == '[') {
                depth++;
            } else if (character == '}' || character == ']') {
                depth--;
            }
        }
        return null;
    }

    private Integer parseNumericFieldValue(String json, int offset) {
        int index = skipWhitespace(json, offset);
        if (index >= json.length() || json.charAt(index) != ':') {
            return null;
        }
        index = skipWhitespace(json, index + 1);
        int start = index;
        if (index < json.length() && json.charAt(index) == '-') {
            index++;
        }
        int digits = index;
        while (index < json.length() && Character.isDigit(json.charAt(index))) {
            index++;
        }
        if (digits == index) {
            return null;
        }
        try {
            return Integer.valueOf(json.substring(start, index));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private int skipWhitespace(String value, int offset) {
        int index = offset;
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private Charset resolveCharset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException exception) {
            return StandardCharsets.UTF_8;
        }
    }

    private String attributeString(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? null : value.toString();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String sanitizeExceptionMessage(String message) {
        return ApiAccessLogMessageSanitizer.sanitize(
                message,
                Math.min(1_024, Math.max(0, properties.getException().getMaxMessageLength())),
                properties.getSensitive().getMaskText()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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
        return UuidUtils.randomUuidWithoutHyphens();
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

    private int responsePrefixLimit() {
        return Math.max(128, properties.getApiAccess().getResponsePrefixLength());
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

    /** 一次请求最终完成时可同时用于技术日志和持久化的安全元数据。 */
    private record RequestCompletionSnapshot(
            String routePattern,
            int httpStatus,
            Integer businessCode,
            boolean success,
            Long handlerDurationMs,
            long durationMs,
            String exceptionType,
            String exceptionMessage
    ) {
    }
}
