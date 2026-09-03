/**
 * @file LoginResponse.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 定义成功登录后返回的 Bearer 令牌与最小用户资料。
 * @logic 不包含密码、手机号、邮箱或 Sa-Token 会话内部数据。
 * @dependencies SystemUserProfile
 * @index_tags auth、login、response
 * @author holic512
 */
package com.pipker.business.api.system.auth;

import com.pipker.business.api.common.model.SystemUserProfile;

/**
 * 登录成功响应数据。
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        SystemUserProfile user
) {
}
