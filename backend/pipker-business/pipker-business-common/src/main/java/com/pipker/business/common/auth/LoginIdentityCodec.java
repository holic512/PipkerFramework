/**
 * @file LoginIdentityCodec.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description 在不依赖 Sa-Token 的前提下编码和解码登录身份。
 * @logic 使用空字符分隔登录域与用户标识，再使用无填充 URL-safe Base64 形成无歧义会话主体。
 * @dependencies LoginIdentity、LoginType、Java 标准库
 * @index_tags auth、identity、codec
 * @author holic512
 */
package com.pipker.business.common.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 登录身份的稳定会话编码器。
 */
public final class LoginIdentityCodec {

    private static final String IDENTITY_SEPARATOR = "\u0000";

    private LoginIdentityCodec() {
    }

    /**
     * 编码登录身份。
     *
     * @param identity 登录身份
     * @return 适合存入会话主体的编码字符串
     */
    public static String encode(LoginIdentity identity) {
        String rawIdentity = identity.loginType().value() + IDENTITY_SEPARATOR + identity.userId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawIdentity.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码登录身份。
     *
     * @param encodedIdentity 会话主体编码
     * @return 已校验的登录身份
     * @throws IllegalArgumentException 编码非法或不符合 Pipker 身份格式时抛出
     */
    public static LoginIdentity decode(String encodedIdentity) {
        try {
            String rawIdentity = new String(
                    Base64.getUrlDecoder().decode(encodedIdentity),
                    StandardCharsets.UTF_8
            );
            int separatorIndex = rawIdentity.indexOf(IDENTITY_SEPARATOR);
            if (separatorIndex <= 0 || separatorIndex != rawIdentity.lastIndexOf(IDENTITY_SEPARATOR)) {
                throw new IllegalArgumentException("encoded identity is not a Pipker identity");
            }
            return new LoginIdentity(
                    new LoginType(rawIdentity.substring(0, separatorIndex)),
                    rawIdentity.substring(separatorIndex + IDENTITY_SEPARATOR.length())
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("encoded identity is not a Pipker identity", exception);
        }
    }
}
