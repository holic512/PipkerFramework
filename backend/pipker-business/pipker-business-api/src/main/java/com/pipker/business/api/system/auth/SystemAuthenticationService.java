/**
 * @file SystemAuthenticationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 编排 system_user 凭证校验、密码升级和 Sa-Token 会话创建。
 * @logic 组合 SecurityCryptoService、系统账户服务和 AuthSessionService，完成凭证校验、密码升级与会话创建。
 * @dependencies SystemAccountService、SecurityCryptoService、AuthSessionService、SystemLoginTypes
 * @index_tags auth、login、security、session
 * @author holic512
 */
package com.pipker.business.api.system.auth;

import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserProfile;
import com.pipker.business.api.system.user.SystemAccountService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.satoken.service.AuthSessionService;
import com.pipker.starter.satoken.service.AuthToken;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统用户认证应用服务。
 */
@Service
public class SystemAuthenticationService {

    private final SystemAccountService systemAccountService;
    private final SecurityCryptoService securityCryptoService;
    private final AuthSessionService authSessionService;

    /**
     * 创建认证服务。
     *
     * @param systemAccountService 系统账户服务
     * @param securityCryptoService 密码学服务
     * @param authSessionService 会话服务
     */
    public SystemAuthenticationService(
            SystemAccountService systemAccountService,
            SecurityCryptoService securityCryptoService,
            AuthSessionService authSessionService
    ) {
        this.systemAccountService = systemAccountService;
        this.securityCryptoService = securityCryptoService;
        this.authSessionService = authSessionService;
    }

    /**
     * 完成系统用户登录。
     *
     * @param request 登录凭证
     * @return Bearer 令牌和公开用户资料
     */
    public LoginResponse login(LoginRequest request) {
        SystemUser user = systemAccountService.findByUsername(request.username().trim());
        if (user == null || !securityCryptoService.matchesPassword(request.password(), user.password())) {
            throw new ApiBusinessException(CommonApiCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new ApiBusinessException(CommonApiCode.AUTH_ACCOUNT_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (securityCryptoService.needsPasswordUpgrade(user.password())) {
            systemAccountService.updatePasswordHash(user.id(), securityCryptoService.hashPassword(request.password()), now);
        }
        AuthToken token = authSessionService.login(
                new LoginIdentity(SystemLoginTypes.SYSTEM, String.valueOf(user.id()))
        );
        systemAccountService.recordSuccessfulLogin(user.id(), now);
        return new LoginResponse(token.value(), "Bearer", SystemUserProfile.from(user));
    }
}
