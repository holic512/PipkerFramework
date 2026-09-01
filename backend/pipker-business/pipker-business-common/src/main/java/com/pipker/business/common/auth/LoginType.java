/**
 * 文件：LoginType.java
 * 项目：Pipker Framework
 * 模块：Pipker Business Common
 * 说明：定义可扩展且经过校验的账户域标识，不强制规定应用角色。
 * 处理逻辑：拒绝空白登录域值，使认证基础设施能够安全区分账户类型，同时保留业务模块对具体语义的控制权。
 * 依赖：Java 标准库
 * 检索关键词：business、common、认证、身份、登录类型
 * 作者：holic512
 */
package com.pipker.business.common.auth;

import java.util.Objects;

public record LoginType(String value) {

    public LoginType {
        Objects.requireNonNull(value, "loginType must not be null");
        value = value.trim();
        if (value.isEmpty() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("loginType must not be blank");
        }
    }
}
