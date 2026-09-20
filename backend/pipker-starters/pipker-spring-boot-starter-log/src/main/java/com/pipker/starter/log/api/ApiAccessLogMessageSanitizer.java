/**
 * @file ApiAccessLogMessageSanitizer.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 将接口访问日志中的异常消息清理为可持久化的短摘要。
 * @logic 移除换行，遮盖常见凭据键值和 Bearer 令牌，再按配置上限截断；原始异常对象和消息不被修改。
 * @dependencies Java Regular Expressions
 * @index_tags log、api-access、exception、sanitization、credentials
 * @author holic512
 */
package com.pipker.starter.log.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 接口访问异常摘要的无状态安全清理器。 */
public final class ApiAccessLogMessageSanitizer {

    private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|token|access[_-]?token|refresh[_-]?token|authorization|cookie)"
                    + "(\\s*[:=]\\s*)(?:Bearer\\s+[^\\s,;&]+|\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;&]+)"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)\\bBearer\\s+[^\\s,;&]+");

    private ApiAccessLogMessageSanitizer() {
    }

    /** 清理、脱敏并截断一条异常消息。 */
    public static String sanitize(String message, int maxLength, String configuredMask) {
        if (message == null) {
            return null;
        }
        String mask = configuredMask == null || configuredMask.isBlank() ? "******" : configuredMask;
        String normalized = message.replace('\r', ' ').replace('\n', ' ');
        Matcher credentials = CREDENTIAL_ASSIGNMENT.matcher(normalized);
        String redacted = credentials.replaceAll(match -> Matcher.quoteReplacement(
                match.group(1) + match.group(2) + mask
        ));
        redacted = BEARER_TOKEN.matcher(redacted).replaceAll("Bearer " + Matcher.quoteReplacement(mask));
        int limit = Math.max(0, maxLength);
        return limit == 0 || redacted.length() <= limit
                ? redacted
                : redacted.substring(0, limit) + "…";
    }
}
