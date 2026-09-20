/**
 * @file AuthenticationAuditRequestContext.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 承载认证 Filter 在进入 MVC 前可安全采集的最小 HTTP 请求元数据。
 * @logic 仅传递客户端地址、User-Agent、方法和路径，不包含 Header 集合、Cookie、请求体或 Authorization Token。
 * @dependencies Java 标准库
 * @index_tags authentication、audit、request-context、security
 * @author holic512
 */
package com.pipker.business.common.auth.audit;

/** Filter 失败路径显式传递的安全请求上下文。 */
public record AuthenticationAuditRequestContext(
        String clientIp,
        String userAgent,
        String httpMethod,
        String requestPath
) {
}
