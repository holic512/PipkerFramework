/**
 * @file LogoutResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义当前 Bearer Token 服务端注销成功后的稳定响应。
 * @logic 只确认当前请求令牌已经失效，不返回令牌内容、会话内部标识或其他设备状态。
 * @dependencies Java 标准库
 * @index_tags auth、logout、response、session
 * @author holic512
 */
package com.pipker.business.api.system.auth.dto;

/** 当前会话注销结果。 */
public record LogoutResponse(boolean loggedOut) {
}
