/**
 * @file LoginLogManagementResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义管理员登录日志分页查询的安全响应投影。
 * @logic 雪花 ID 统一转为字符串，返回认证结果与受控请求元数据，不返回任何密码、令牌、Cookie 或请求体。
 * @dependencies Java 标准库
 * @index_tags authentication、audit、login-log、response、pagination
 * @author holic512
 */
package com.pipker.business.api.system.loginlog;

import java.time.LocalDateTime;
import java.util.List;

/** 登录日志管理响应模型集合。 */
public final class LoginLogManagementResponse {

    private LoginLogManagementResponse() {
    }

    /** 登录日志列表项。 */
    public record LoginLogSummary(
            String id,
            String userId,
            String username,
            String eventType,
            String result,
            String failureReason,
            String clientIp,
            String userAgent,
            String httpMethod,
            String requestPath,
            String traceId,
            LocalDateTime occurredAt
    ) {
    }

    /** 不暴露 MyBatis-Plus 内部字段的分页结果。 */
    public record PageResult<T>(long page, long pageSize, long total, List<T> records) {
    }
}
