/**
 * @file SystemApiAccessLog.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射接口访问性能与结果元数据表，不承载请求参数、请求体或响应体。
 * @logic 异步数据库 Handler 追加记录，管理服务只读筛选和聚合，定时任务按发生时间分批清理。
 * @dependencies MyBatis-Plus、Lombok、Java Time
 * @index_tags api-log、access-log、observability、persistence
 * @author holic512
 */
package com.pipker.business.api.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 接口访问日志持久化模型。 */
@Getter
@Setter
@TableName("system_api_access_log")
public class SystemApiAccessLog {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("occurred_at")
    private LocalDateTime occurredAt;
    @TableField("occurred_hour")
    private LocalDateTime occurredHour;
    @TableField("occurred_date")
    private LocalDate occurredDate;
    @TableField("service_name")
    private String serviceName;
    @TableField("http_method")
    private String httpMethod;
    @TableField("request_path")
    private String requestPath;
    @TableField("route_pattern")
    private String routePattern;
    @TableField("handler_method")
    private String handlerMethod;
    @TableField("purpose")
    private String purpose;
    @TableField("remark")
    private String remark;
    @TableField("http_status")
    private Integer httpStatus;
    @TableField("business_code")
    private Integer businessCode;
    @TableField("success")
    private Boolean success;
    @TableField("handler_duration_ms")
    private Long handlerDurationMs;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("trace_id")
    private String traceId;
    @TableField("request_id")
    private String requestId;
    @TableField("client_ip")
    private String clientIp;
    @TableField("exception_type")
    private String exceptionType;
    @TableField("exception_message")
    private String exceptionMessage;
}
