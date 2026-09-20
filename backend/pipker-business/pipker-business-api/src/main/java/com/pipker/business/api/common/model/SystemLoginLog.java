/**
 * @file SystemLoginLog.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 映射 system_login_log 中不可变的认证生命周期审计记录。
 * @logic 使用雪花主键保存安全认证结果和受长度约束的请求元数据，不建立用户外键，也不承载密码、令牌或请求体。
 * @dependencies MyBatis-Plus、Lombok、Java 标准库
 * @index_tags authentication、audit、login-log、persistence、mybatis-plus
 * @author holic512
 */
package com.pipker.business.api.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 认证生命周期日志持久化模型。 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TableName("system_login_log")
public class SystemLoginLog {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("event_type")
    private String eventType;

    @TableField("result")
    private String result;

    @TableField("failure_reason")
    private String failureReason;

    @TableField("client_ip")
    private String clientIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("http_method")
    private String httpMethod;

    @TableField("request_path")
    private String requestPath;

    @TableField("trace_id")
    private String traceId;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;
}
