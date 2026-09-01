/**
 * @file PipkerLogProperties.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 绑定 pipker.log 下的统一日志基础设施配置和安全默认值。
 * @logic 集中定义 Trace、请求、慢请求、操作、脱敏和异常日志的开关及参数，避免实现层写死运行策略。
 * @dependencies Spring Boot Configuration Properties、LogLevel、SensitiveType
 * @index_tags log、configuration、starter
 * @author holic512
 */
package com.pipker.starter.log.config;

import com.pipker.starter.log.sensitive.SensitiveType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("pipker.log")
public class PipkerLogProperties {

    private boolean enabled = true;
    private Context context = new Context();
    private Trace trace = new Trace();
    private Request request = new Request();
    private SlowRequest slowRequest = new SlowRequest();
    private Operation operation = new Operation();
    private Sensitive sensitive = new Sensitive();
    private Exception exception = new Exception();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Trace getTrace() {
        return trace;
    }

    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public SlowRequest getSlowRequest() {
        return slowRequest;
    }

    public void setSlowRequest(SlowRequest slowRequest) {
        this.slowRequest = slowRequest;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Sensitive getSensitive() {
        return sensitive;
    }

    public void setSensitive(Sensitive sensitive) {
        this.sensitive = sensitive;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public static class Context {

        private String serviceName;
        private boolean requestIdEnabled = true;
        private boolean clientIpEnabled = true;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public boolean isRequestIdEnabled() {
            return requestIdEnabled;
        }

        public void setRequestIdEnabled(boolean requestIdEnabled) {
            this.requestIdEnabled = requestIdEnabled;
        }

        public boolean isClientIpEnabled() {
            return clientIpEnabled;
        }

        public void setClientIpEnabled(boolean clientIpEnabled) {
            this.clientIpEnabled = clientIpEnabled;
        }
    }

    public static class Trace {

        private boolean enabled = true;
        private String headerName = "X-Trace-Id";
        private boolean acceptUpstream = true;
        private boolean writeResponseHeader = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public boolean isAcceptUpstream() {
            return acceptUpstream;
        }

        public void setAcceptUpstream(boolean acceptUpstream) {
            this.acceptUpstream = acceptUpstream;
        }

        public boolean isWriteResponseHeader() {
            return writeResponseHeader;
        }

        public void setWriteResponseHeader(boolean writeResponseHeader) {
            this.writeResponseHeader = writeResponseHeader;
        }
    }

    public static class Request {

        private boolean enabled;
        private LogLevel level = LogLevel.INFO;
        private boolean includeParameters;
        private boolean includeHeaders;
        private boolean includeRequestBody;
        private boolean includeResponseBody;
        private int maxBodyLength = 4_096;
        private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 10;
        private List<String> ignoredPaths = new ArrayList<>(List.of(
                "/actuator/**", "/error", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/webjars/**"
        ));
        private List<String> ignoredContentTypes = new ArrayList<>(List.of(
                "multipart/*", "application/octet-stream", "application/pdf", "image/*", "video/*", "audio/*"
        ));
        private List<String> bodyContentTypes = new ArrayList<>(List.of("application/json", "application/*+json"));

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public LogLevel getLevel() {
            return level;
        }

        public void setLevel(LogLevel level) {
            this.level = level;
        }

        public boolean isIncludeParameters() {
            return includeParameters;
        }

        public void setIncludeParameters(boolean includeParameters) {
            this.includeParameters = includeParameters;
        }

        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        public boolean isIncludeRequestBody() {
            return includeRequestBody;
        }

        public void setIncludeRequestBody(boolean includeRequestBody) {
            this.includeRequestBody = includeRequestBody;
        }

        public boolean isIncludeResponseBody() {
            return includeResponseBody;
        }

        public void setIncludeResponseBody(boolean includeResponseBody) {
            this.includeResponseBody = includeResponseBody;
        }

        public int getMaxBodyLength() {
            return maxBodyLength;
        }

        public void setMaxBodyLength(int maxBodyLength) {
            this.maxBodyLength = maxBodyLength;
        }

        public int getFilterOrder() {
            return filterOrder;
        }

        public void setFilterOrder(int filterOrder) {
            this.filterOrder = filterOrder;
        }

        public List<String> getIgnoredPaths() {
            return ignoredPaths;
        }

        public void setIgnoredPaths(List<String> ignoredPaths) {
            this.ignoredPaths = ignoredPaths;
        }

        public List<String> getIgnoredContentTypes() {
            return ignoredContentTypes;
        }

        public void setIgnoredContentTypes(List<String> ignoredContentTypes) {
            this.ignoredContentTypes = ignoredContentTypes;
        }

        public List<String> getBodyContentTypes() {
            return bodyContentTypes;
        }

        public void setBodyContentTypes(List<String> bodyContentTypes) {
            this.bodyContentTypes = bodyContentTypes;
        }
    }

    public static class SlowRequest {

        private boolean enabled = true;
        private long threshold = 1_000;
        private LogLevel level = LogLevel.WARN;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getThreshold() {
            return threshold;
        }

        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }

        public LogLevel getLevel() {
            return level;
        }

        public void setLevel(LogLevel level) {
            this.level = level;
        }
    }

    public static class Operation {

        private boolean enabled = true;
        private LogLevel level = LogLevel.INFO;
        private boolean recordParameters;
        private boolean recordResult;
        private int maxValueLength = 4_096;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public LogLevel getLevel() {
            return level;
        }

        public void setLevel(LogLevel level) {
            this.level = level;
        }

        public boolean isRecordParameters() {
            return recordParameters;
        }

        public void setRecordParameters(boolean recordParameters) {
            this.recordParameters = recordParameters;
        }

        public boolean isRecordResult() {
            return recordResult;
        }

        public void setRecordResult(boolean recordResult) {
            this.recordResult = recordResult;
        }

        public int getMaxValueLength() {
            return maxValueLength;
        }

        public void setMaxValueLength(int maxValueLength) {
            this.maxValueLength = maxValueLength;
        }
    }

    public static class Sensitive {

        private boolean enabled = true;
        private String maskText = "******";
        private Map<String, SensitiveType> additionalFields = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMaskText() {
            return maskText;
        }

        public void setMaskText(String maskText) {
            this.maskText = maskText;
        }

        public Map<String, SensitiveType> getAdditionalFields() {
            return additionalFields;
        }

        public void setAdditionalFields(Map<String, SensitiveType> additionalFields) {
            this.additionalFields = additionalFields;
        }
    }

    public static class Exception {

        private boolean enabled = true;
        private int maxMessageLength = 1_024;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }
    }
}
