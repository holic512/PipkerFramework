/**
 * 文件：SaTokenAuthSessionService.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Sa-Token
 * 说明：通过 Sa-Token 默认登录域实现公开的认证会话门面。
 * 处理逻辑：将可扩展的 LoginIdentity 编码为不含分隔符歧义的 Sa-Token 登录 ID，将生命周期操作委托给 StpUtil，并为调用方解析当前主体。
 * 依赖：Pipker 认证契约、Sa-Token Core
 * 检索关键词：starter、sa-token、认证、会话、身份
 * 作者：holic512
 */
package com.pipker.starter.satoken.service;

import cn.dev33.satoken.stp.StpUtil;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.LoginType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class SaTokenAuthSessionService implements AuthSessionService {

    private static final String IDENTITY_SEPARATOR = "\u0000";

    @Override
    public AuthToken login(LoginIdentity identity) {
        StpUtil.login(encode(identity));
        String tokenValue = StpUtil.getTokenValue();
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalStateException("Sa-Token did not create a token for the current request");
        }
        return new AuthToken(tokenValue, identity);
    }

    @Override
    public void logoutCurrent() {
        StpUtil.logout();
    }

    @Override
    public Optional<LoginIdentity> currentIdentity() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (loginId == null) {
            return Optional.empty();
        }
        return Optional.of(decode(String.valueOf(loginId)));
    }

    @Override
    public void checkLogin() {
        StpUtil.checkLogin();
    }

    private String encode(LoginIdentity identity) {
        String rawIdentity = identity.loginType().value() + IDENTITY_SEPARATOR + identity.userId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawIdentity.getBytes(StandardCharsets.UTF_8));
    }

    private LoginIdentity decode(String encodedIdentity) {
        try {
            String rawIdentity = new String(
                    Base64.getUrlDecoder().decode(encodedIdentity),
                    StandardCharsets.UTF_8
            );
            int separatorIndex = rawIdentity.indexOf(IDENTITY_SEPARATOR);
            if (separatorIndex <= 0 || separatorIndex != rawIdentity.lastIndexOf(IDENTITY_SEPARATOR)) {
                throw new IllegalStateException("Current Sa-Token login ID is not a Pipker identity");
            }
            return new LoginIdentity(
                    new LoginType(rawIdentity.substring(0, separatorIndex)),
                    rawIdentity.substring(separatorIndex + IDENTITY_SEPARATOR.length())
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Current Sa-Token login ID is not a Pipker identity", exception);
        }
    }
}
