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

/**
 * 绑定 {@code pipker.log} 下的日志基础设施配置。
 */
@ConfigurationProperties("pipker.log")
public class PipkerLogProperties {

    /**
     * 日志 Starter 总开关，默认为启用。
     */
    private boolean enabled = true;
    /**
     * 请求上下文配置。
     */
    private Context context = new Context();
    /**
     * TraceId 配置。
     */
    private Trace trace = new Trace();
    /**
     * 普通 HTTP 请求日志配置。
     */
    private Request request = new Request();
    /**
     * 慢请求日志配置。
     */
    private SlowRequest slowRequest = new SlowRequest();
    /**
     * 业务操作日志配置。
     */
    private Operation operation = new Operation();
    /**
     * 日志脱敏配置。
     */
    private Sensitive sensitive = new Sensitive();
    /**
     * 异常日志配置。
     */
    private Exception exception = new Exception();

    /**
     * 返回日志 Starter 总开关。
     *
     * @return 是否启用日志 Starter
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置日志 Starter 总开关。
     *
     * @param enabled 是否启用日志 Starter
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回请求上下文配置。
     *
     * @return 请求上下文配置
     */
    public Context getContext() {
        return context;
    }

    /**
     * 设置请求上下文配置。
     *
     * @param context 请求上下文配置
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * 返回 TraceId 配置。
     *
     * @return TraceId 配置
     */
    public Trace getTrace() {
        return trace;
    }

    /**
     * 设置 TraceId 配置。
     *
     * @param trace TraceId 配置
     */
    public void setTrace(Trace trace) {
        this.trace = trace;
    }

    /**
     * 返回普通 HTTP 请求日志配置。
     *
     * @return 请求日志配置
     */
    public Request getRequest() {
        return request;
    }

    /**
     * 设置普通 HTTP 请求日志配置。
     *
     * @param request 请求日志配置
     */
    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * 返回慢请求日志配置。
     *
     * @return 慢请求日志配置
     */
    public SlowRequest getSlowRequest() {
        return slowRequest;
    }

    /**
     * 设置慢请求日志配置。
     *
     * @param slowRequest 慢请求日志配置
     */
    public void setSlowRequest(SlowRequest slowRequest) {
        this.slowRequest = slowRequest;
    }

    /**
     * 返回业务操作日志配置。
     *
     * @return 操作日志配置
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * 设置业务操作日志配置。
     *
     * @param operation 操作日志配置
     */
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    /**
     * 返回日志脱敏配置。
     *
     * @return 脱敏配置
     */
    public Sensitive getSensitive() {
        return sensitive;
    }

    /**
     * 设置日志脱敏配置。
     *
     * @param sensitive 脱敏配置
     */
    public void setSensitive(Sensitive sensitive) {
        this.sensitive = sensitive;
    }

    /**
     * 返回异常日志配置。
     *
     * @return 异常日志配置
     */
    public Exception getException() {
        return exception;
    }

    /**
     * 设置异常日志配置。
     *
     * @param exception 异常日志配置
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    /**
     * 定义服务名称、请求 ID 和客户端 IP 等基础上下文配置。
     */
    public static class Context {

        /**
         * 覆盖默认的服务名称；为空时使用 {@code spring.application.name}。
         */
        private String serviceName;
        /**
         * 是否生成请求 ID，默认为启用。
         */
        private boolean requestIdEnabled = true;
        /**
         * 是否写入客户端 IP，默认为启用。
         */
        private boolean clientIpEnabled = true;

        /**
         * 返回配置的服务名称。
         *
         * @return 服务名称
         */
        public String getServiceName() {
            return serviceName;
        }

        /**
         * 设置服务名称。
         *
         * @param serviceName 服务名称
         */
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * 返回请求 ID 开关。
         *
         * @return 是否生成请求 ID
         */
        public boolean isRequestIdEnabled() {
            return requestIdEnabled;
        }

        /**
         * 设置请求 ID 开关。
         *
         * @param requestIdEnabled 是否生成请求 ID
         */
        public void setRequestIdEnabled(boolean requestIdEnabled) {
            this.requestIdEnabled = requestIdEnabled;
        }

        /**
         * 返回客户端 IP 开关。
         *
         * @return 是否记录客户端 IP
         */
        public boolean isClientIpEnabled() {
            return clientIpEnabled;
        }

        /**
         * 设置客户端 IP 开关。
         *
         * @param clientIpEnabled 是否记录客户端 IP
         */
        public void setClientIpEnabled(boolean clientIpEnabled) {
            this.clientIpEnabled = clientIpEnabled;
        }
    }

    /**
     * 定义 TraceId 的生成、接收和响应头回写策略。
     */
    public static class Trace {

        /**
         * TraceId 能力开关，默认为启用。
         */
        private boolean enabled = true;
        /**
         * TraceId 请求头名称，默认为 {@code X-Trace-Id}。
         */
        private String headerName = "X-Trace-Id";
        /**
         * 是否接受上游传入的合法 TraceId，默认为启用。
         */
        private boolean acceptUpstream = true;
        /**
         * 是否将 TraceId 写回响应头，默认为启用。
         */
        private boolean writeResponseHeader = true;

        /**
         * 返回 TraceId 能力开关。
         *
         * @return 是否启用 TraceId
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 TraceId 能力开关。
         *
         * @param enabled 是否启用 TraceId
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回 TraceId 请求头名称。
         *
         * @return 请求头名称
         */
        public String getHeaderName() {
            return headerName;
        }

        /**
         * 设置 TraceId 请求头名称。
         *
         * @param headerName 请求头名称
         */
        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        /**
         * 返回是否接受上游 TraceId。
         *
         * @return 是否接受上游 TraceId
         */
        public boolean isAcceptUpstream() {
            return acceptUpstream;
        }

        /**
         * 设置是否接受上游 TraceId。
         *
         * @param acceptUpstream 是否接受上游 TraceId
         */
        public void setAcceptUpstream(boolean acceptUpstream) {
            this.acceptUpstream = acceptUpstream;
        }

        /**
         * 返回是否写回响应头。
         *
         * @return 是否写回 TraceId 响应头
         */
        public boolean isWriteResponseHeader() {
            return writeResponseHeader;
        }

        /**
         * 设置是否写回响应头。
         *
         * @param writeResponseHeader 是否写回 TraceId 响应头
         */
        public void setWriteResponseHeader(boolean writeResponseHeader) {
            this.writeResponseHeader = writeResponseHeader;
        }
    }

    /**
     * 定义普通 HTTP 请求日志的记录内容、长度限制和忽略规则。
     */
    public static class Request {

        /**
         * 普通请求日志开关，默认为关闭。
         */
        private boolean enabled;
        /**
         * 普通请求日志等级，默认为 {@code INFO}。
         */
        private LogLevel level = LogLevel.INFO;
        /**
         * 是否记录请求参数，默认为关闭。
         */
        private boolean includeParameters;
        /**
         * 是否记录请求头，默认为关闭。
         */
        private boolean includeHeaders;
        /**
         * 是否记录 JSON 请求体，默认为关闭。
         */
        private boolean includeRequestBody;
        /**
         * 是否记录 JSON 响应体，默认为关闭。
         */
        private boolean includeResponseBody;
        /**
         * 请求和响应体的最大捕获长度，默认为 4096 字节。
         */
        private int maxBodyLength = 4_096;
        /**
         * 请求日志 Filter 的注册顺序。
         */
        private int filterOrder = Ordered.HIGHEST_PRECEDENCE + 10;
        /**
         * 不记录请求日志的路径模式。
         */
        private List<String> ignoredPaths = new ArrayList<>(List.of(
                "/actuator/**", "/error", "/favicon.ico", "/css/**", "/js/**", "/images/**", "/webjars/**"
        ));
        /**
         * 不读取请求或响应体的媒体类型模式。
         */
        private List<String> ignoredContentTypes = new ArrayList<>(List.of(
                "multipart/*", "application/octet-stream", "application/pdf", "image/*", "video/*", "audio/*"
        ));
        /**
         * 允许读取并解析请求或响应体的媒体类型模式。
         */
        private List<String> bodyContentTypes = new ArrayList<>(List.of("application/json", "application/*+json"));

        /**
         * 返回普通请求日志开关。
         *
         * @return 是否记录普通请求日志
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置普通请求日志开关。
         *
         * @param enabled 是否记录普通请求日志
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回普通请求日志等级。
         *
         * @return 日志等级
         */
        public LogLevel getLevel() {
            return level;
        }

        /**
         * 设置普通请求日志等级。
         *
         * @param level 日志等级
         */
        public void setLevel(LogLevel level) {
            this.level = level;
        }

        /**
         * 返回是否记录请求参数。
         *
         * @return 是否记录请求参数
         */
        public boolean isIncludeParameters() {
            return includeParameters;
        }

        /**
         * 设置是否记录请求参数。
         *
         * @param includeParameters 是否记录请求参数
         */
        public void setIncludeParameters(boolean includeParameters) {
            this.includeParameters = includeParameters;
        }

        /**
         * 返回是否记录请求头。
         *
         * @return 是否记录请求头
         */
        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        /**
         * 设置是否记录请求头。
         *
         * @param includeHeaders 是否记录请求头
         */
        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        /**
         * 返回是否记录请求体。
         *
         * @return 是否记录请求体
         */
        public boolean isIncludeRequestBody() {
            return includeRequestBody;
        }

        /**
         * 设置是否记录请求体。
         *
         * @param includeRequestBody 是否记录请求体
         */
        public void setIncludeRequestBody(boolean includeRequestBody) {
            this.includeRequestBody = includeRequestBody;
        }

        /**
         * 返回是否记录响应体。
         *
         * @return 是否记录响应体
         */
        public boolean isIncludeResponseBody() {
            return includeResponseBody;
        }

        /**
         * 设置是否记录响应体。
         *
         * @param includeResponseBody 是否记录响应体
         */
        public void setIncludeResponseBody(boolean includeResponseBody) {
            this.includeResponseBody = includeResponseBody;
        }

        /**
         * 返回请求和响应体最大捕获长度。
         *
         * @return 最大长度，单位为字节
         */
        public int getMaxBodyLength() {
            return maxBodyLength;
        }

        /**
         * 设置请求和响应体最大捕获长度。
         *
         * @param maxBodyLength 最大长度，单位为字节
         */
        public void setMaxBodyLength(int maxBodyLength) {
            this.maxBodyLength = maxBodyLength;
        }

        /**
         * 返回请求日志 Filter 的注册顺序。
         *
         * @return Filter 顺序值
         */
        public int getFilterOrder() {
            return filterOrder;
        }

        /**
         * 设置请求日志 Filter 的注册顺序。
         *
         * @param filterOrder Filter 顺序值
         */
        public void setFilterOrder(int filterOrder) {
            this.filterOrder = filterOrder;
        }

        /**
         * 返回忽略路径模式。
         *
         * @return 忽略路径模式列表
         */
        public List<String> getIgnoredPaths() {
            return ignoredPaths;
        }

        /**
         * 设置忽略路径模式。
         *
         * @param ignoredPaths 忽略路径模式列表
         */
        public void setIgnoredPaths(List<String> ignoredPaths) {
            this.ignoredPaths = ignoredPaths;
        }

        /**
         * 返回不读取内容体的媒体类型模式。
         *
         * @return 忽略媒体类型模式列表
         */
        public List<String> getIgnoredContentTypes() {
            return ignoredContentTypes;
        }

        /**
         * 设置不读取内容体的媒体类型模式。
         *
         * @param ignoredContentTypes 忽略媒体类型模式列表
         */
        public void setIgnoredContentTypes(List<String> ignoredContentTypes) {
            this.ignoredContentTypes = ignoredContentTypes;
        }

        /**
         * 返回允许读取内容体的媒体类型模式。
         *
         * @return 内容体媒体类型模式列表
         */
        public List<String> getBodyContentTypes() {
            return bodyContentTypes;
        }

        /**
         * 设置允许读取内容体的媒体类型模式。
         *
         * @param bodyContentTypes 内容体媒体类型模式列表
         */
        public void setBodyContentTypes(List<String> bodyContentTypes) {
            this.bodyContentTypes = bodyContentTypes;
        }
    }

    /**
     * 定义慢请求的启用状态、阈值和输出等级。
     */
    public static class SlowRequest {

        /**
         * 慢请求日志开关，默认为启用。
         */
        private boolean enabled = true;
        /**
         * 慢请求阈值，默认为 1000 毫秒。
         */
        private long threshold = 1_000;
        /**
         * 慢请求日志等级，默认为 {@code WARN}。
         */
        private LogLevel level = LogLevel.WARN;

        /**
         * 返回慢请求日志开关。
         *
         * @return 是否记录慢请求
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置慢请求日志开关。
         *
         * @param enabled 是否记录慢请求
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回慢请求阈值。
         *
         * @return 阈值，单位为毫秒
         */
        public long getThreshold() {
            return threshold;
        }

        /**
         * 设置慢请求阈值。
         *
         * @param threshold 阈值，单位为毫秒
         */
        public void setThreshold(long threshold) {
            this.threshold = threshold;
        }

        /**
         * 返回慢请求日志等级。
         *
         * @return 日志等级
         */
        public LogLevel getLevel() {
            return level;
        }

        /**
         * 设置慢请求日志等级。
         *
         * @param level 日志等级
         */
        public void setLevel(LogLevel level) {
            this.level = level;
        }
    }

    /**
     * 定义业务操作日志的记录开关、等级和长度限制。
     */
    public static class Operation {

        /**
         * 操作日志开关，默认为启用。
         */
        private boolean enabled = true;
        /**
         * 操作日志等级，默认为 {@code INFO}。
         */
        private LogLevel level = LogLevel.INFO;
        /**
         * 是否默认记录方法参数，默认为关闭。
         */
        private boolean recordParameters;
        /**
         * 是否默认记录方法返回值，默认为关闭。
         */
        private boolean recordResult;
        /**
         * 参数和返回值的最大渲染长度，默认为 4096 个字符。
         */
        private int maxValueLength = 4_096;

        /**
         * 返回操作日志开关。
         *
         * @return 是否记录操作日志
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置操作日志开关。
         *
         * @param enabled 是否记录操作日志
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回操作日志等级。
         *
         * @return 日志等级
         */
        public LogLevel getLevel() {
            return level;
        }

        /**
         * 设置操作日志等级。
         *
         * @param level 日志等级
         */
        public void setLevel(LogLevel level) {
            this.level = level;
        }

        /**
         * 返回默认参数记录开关。
         *
         * @return 是否默认记录参数
         */
        public boolean isRecordParameters() {
            return recordParameters;
        }

        /**
         * 设置默认参数记录开关。
         *
         * @param recordParameters 是否默认记录参数
         */
        public void setRecordParameters(boolean recordParameters) {
            this.recordParameters = recordParameters;
        }

        /**
         * 返回默认返回值记录开关。
         *
         * @return 是否默认记录返回值
         */
        public boolean isRecordResult() {
            return recordResult;
        }

        /**
         * 设置默认返回值记录开关。
         *
         * @param recordResult 是否默认记录返回值
         */
        public void setRecordResult(boolean recordResult) {
            this.recordResult = recordResult;
        }

        /**
         * 返回参数和返回值的最大渲染长度。
         *
         * @return 最大长度
         */
        public int getMaxValueLength() {
            return maxValueLength;
        }

        /**
         * 设置参数和返回值的最大渲染长度。
         *
         * @param maxValueLength 最大长度
         */
        public void setMaxValueLength(int maxValueLength) {
            this.maxValueLength = maxValueLength;
        }
    }

    /**
     * 定义日志脱敏总开关、掩码文本和自定义字段规则。
     */
    public static class Sensitive {

        /**
         * 脱敏能力开关，默认为启用。
         */
        private boolean enabled = true;
        /**
         * 默认掩码文本，默认为六个星号。
         */
        private String maskText = "******";
        /**
         * 用户补充的字段名与敏感类型映射。
         */
        private Map<String, SensitiveType> additionalFields = new LinkedHashMap<>();

        /**
         * 返回脱敏能力开关。
         *
         * @return 是否启用脱敏
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置脱敏能力开关。
         *
         * @param enabled 是否启用脱敏
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回默认掩码文本。
         *
         * @return 掩码文本
         */
        public String getMaskText() {
            return maskText;
        }

        /**
         * 设置默认掩码文本。
         *
         * @param maskText 掩码文本
         */
        public void setMaskText(String maskText) {
            this.maskText = maskText;
        }

        /**
         * 返回自定义字段脱敏规则。
         *
         * @return 字段名与敏感类型映射
         */
        public Map<String, SensitiveType> getAdditionalFields() {
            return additionalFields;
        }

        /**
         * 设置自定义字段脱敏规则。
         *
         * @param additionalFields 字段名与敏感类型映射
         */
        public void setAdditionalFields(Map<String, SensitiveType> additionalFields) {
            this.additionalFields = additionalFields;
        }
    }

    /**
     * 定义 HTTP 异常日志开关和异常消息长度限制。
     */
    public static class Exception {

        /**
         * 异常日志开关，默认为启用。
         */
        private boolean enabled = true;
        /**
         * 异常消息最大长度，默认为 1024 个字符。
         */
        private int maxMessageLength = 1_024;

        /**
         * 返回异常日志开关。
         *
         * @return 是否记录异常日志
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置异常日志开关。
         *
         * @param enabled 是否记录异常日志
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 返回异常消息最大长度。
         *
         * @return 最大长度
         */
        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        /**
         * 设置异常消息最大长度。
         *
         * @param maxMessageLength 最大长度
         */
        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }
    }
}
