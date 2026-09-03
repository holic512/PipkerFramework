/**
 * @file LoginRequest.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 定义 POST /api/auth/login 的最小凭证输入。
 * @logic 在进入认证编排前拒绝空白用户名和密码，且不将该类型记录到任何响应中。
 * @dependencies Jakarta Validation
 * @index_tags auth、login、request
 * @author holic512
 */
package com.pipker.server.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。
 */
public record LoginRequest(
        @NotBlank(message = "username must not be blank") String username,
        @NotBlank(message = "password must not be blank") String password
) {
}
