/**
 * @file SaTokenAuthSessionService.java
 * @project Pipker Framework
 * @module Pipker Sa-Token Starter
 * @description Implements the public authentication-session facade through Sa-Token's default StpUtil domain.
 * @logic Delegates identity encoding and decoding to the business-common codec, delegates lifecycle actions to StpUtil, and never contains role or database authorization logic.
 * @dependencies Sa-Token Core, LoginIdentity, LoginIdentityCodec
 * @index_tags starter, sa-token, authentication, session, identity
 * @author holic512
 */
package com.pipker.starter.satoken.service;

import cn.dev33.satoken.stp.StpUtil;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.LoginIdentityCodec;

import java.util.Optional;

/**
 * 基于 Sa-Token 默认登录域实现认证会话门面。
 */
public class SaTokenAuthSessionService implements AuthSessionService {

    /**
     * 编码身份并通过 Sa-Token 创建登录会话。
     *
     * @param identity 已认证的登录身份
     * @return 新创建的认证令牌
     * @throws IllegalStateException Sa-Token 未能生成当前会话令牌时抛出
     */
    @Override
    public AuthToken login(LoginIdentity identity) {
        StpUtil.login(LoginIdentityCodec.encode(identity));
        String tokenValue = StpUtil.getTokenValue();
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalStateException("Sa-Token did not create a token for the current request");
        }
        return new AuthToken(tokenValue, identity);
    }

    /**
     * 注销当前请求的 Sa-Token 会话。
     */
    @Override
    public void logoutCurrent() {
        StpUtil.logout();
    }

    /**
     * 读取并解码当前 Sa-Token 中保存的 Pipker 登录身份。
     *
     * @return 当前登录身份；未登录时为空
     */
    @Override
    public Optional<LoginIdentity> currentIdentity() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LoginIdentityCodec.decode(String.valueOf(loginId)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Current Sa-Token login ID is not a Pipker identity", exception);
        }
    }

    /**
     * 委托 Sa-Token 检查当前请求是否已登录。
     */
    @Override
    public void checkLogin() {
        StpUtil.checkLogin();
    }

}
