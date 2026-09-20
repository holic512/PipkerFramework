/**
 * @file SystemAuthenticationService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 编排 system_user 凭证校验、密码升级、Sa-Token 会话创建与当前令牌注销，并发布认证审计事件。
 * @logic 登录统一映射成功和失败结果后记录安全事件；登出先读取当前身份再只注销当前 Token，审计 Recorder 故障始终被隔离。
 * @dependencies SystemAccountService、SecurityCryptoService、AuthSessionService、AuthenticationAuditRecorder、SystemLoginTypes
 * @index_tags auth、login、logout、security、session、audit
 * @author holic512
 */
package com.pipker.business.api.system.auth;

import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserProfile;
import com.pipker.business.api.system.auth.dto.LoginRequest;
import com.pipker.business.api.system.auth.dto.LoginResponse;
import com.pipker.business.api.system.auth.dto.LogoutResponse;
import com.pipker.business.api.system.auth.dto.SystemLoginTypes;
import com.pipker.business.api.system.user.SystemAccountService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.EventType;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.FailureReason;
import com.pipker.business.common.auth.audit.AuthenticationAuditRecorder;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.satoken.service.AuthSessionService;
import com.pipker.starter.satoken.service.AuthToken;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 系统用户认证应用服务。
 */
@Service
public class SystemAuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemAuthenticationService.class);

    private final SystemAccountService systemAccountService;
    private final SecurityCryptoService securityCryptoService;
    private final AuthSessionService authSessionService;
    private final AuthenticationAuditRecorder authenticationAuditRecorder;

    /**
     * 创建认证服务。
     *
     * @param systemAccountService 系统账户服务
     * @param securityCryptoService 密码学服务
     * @param authSessionService 会话服务
     * @param authenticationAuditRecorder 认证审计记录器
     */
    public SystemAuthenticationService(
            SystemAccountService systemAccountService,
            SecurityCryptoService securityCryptoService,
            AuthSessionService authSessionService,
            AuthenticationAuditRecorder authenticationAuditRecorder
    ) {
        this.systemAccountService = systemAccountService;
        this.securityCryptoService = securityCryptoService;
        this.authSessionService = authSessionService;
        this.authenticationAuditRecorder = authenticationAuditRecorder;
    }

    /**
     * 完成系统用户登录。
     *
     * @param request 登录凭证
     * @return Bearer 令牌和公开用户资料
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        SystemUser user = null;
        try {
            user = systemAccountService.findByUsername(username);
            if (user == null || !securityCryptoService.matchesPassword(request.password(), user.getPasswordHash())) {
                throw new ApiBusinessException(CommonApiCode.AUTH_INVALID_CREDENTIALS);
            }
            if (!user.isEnabled()) {
                throw new ApiBusinessException(CommonApiCode.AUTH_ACCOUNT_DISABLED);
            }

            LocalDateTime now = LocalDateTime.now();
            if (securityCryptoService.needsPasswordUpgrade(user.getPasswordHash())) {
                systemAccountService.updatePasswordHash(
                        user.getId(), securityCryptoService.hashPassword(request.password()), now
                );
            }
            AuthToken token = authSessionService.login(
                    new LoginIdentity(SystemLoginTypes.SYSTEM, String.valueOf(user.getId()))
            );
            systemAccountService.recordSuccessfulLogin(user.getId(), now);
            recordAudit(AuthenticationAuditEvent.success(EventType.LOGIN, user.getId(), user.getUsername()));
            return new LoginResponse(token.value(), "Bearer", SystemUserProfile.from(user));
        } catch (ApiBusinessException exception) {
            recordAudit(AuthenticationAuditEvent.failure(
                    EventType.LOGIN,
                    mapFailureReason(exception),
                    user == null ? null : user.getId(),
                    user == null ? username : user.getUsername()
            ));
            throw exception;
        } catch (RuntimeException exception) {
            recordAudit(AuthenticationAuditEvent.failure(
                    EventType.LOGIN,
                    FailureReason.INTERNAL_ERROR,
                    user == null ? null : user.getId(),
                    user == null ? username : user.getUsername()
            ));
            throw exception;
        }
    }

    /** 注销当前请求携带的 Token，其他并发 Token 保持有效。 */
    public LogoutResponse logoutCurrent() {
        LoginIdentity identity = null;
        SystemUser user = null;
        try {
            identity = authSessionService.currentIdentity()
                    .orElseThrow(() -> new ApiBusinessException(CommonApiCode.AUTH_REQUIRED));
            Long userId = systemUserId(identity);
            user = userId == null ? null : systemAccountService.findById(userId);
            authSessionService.logoutCurrent();
            recordAudit(AuthenticationAuditEvent.success(
                    EventType.LOGOUT,
                    userId,
                    user == null ? null : user.getUsername()
            ));
            return new LogoutResponse(true);
        } catch (ApiBusinessException exception) {
            recordAudit(AuthenticationAuditEvent.failure(
                    EventType.LOGOUT,
                    mapFailureReason(exception),
                    systemUserId(identity),
                    user == null ? null : user.getUsername()
            ));
            throw exception;
        } catch (RuntimeException exception) {
            recordAudit(AuthenticationAuditEvent.failure(
                    EventType.LOGOUT,
                    FailureReason.INTERNAL_ERROR,
                    systemUserId(identity),
                    user == null ? null : user.getUsername()
            ));
            throw exception;
        }
    }

    private Long systemUserId(LoginIdentity identity) {
        if (identity == null || !SystemLoginTypes.SYSTEM.equals(identity.loginType())) {
            return null;
        }
        try {
            long userId = Long.parseLong(identity.userId());
            return userId > 0 ? userId : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private FailureReason mapFailureReason(ApiBusinessException exception) {
        if (exception.getCode() == CommonApiCode.AUTH_INVALID_CREDENTIALS) {
            return FailureReason.INVALID_CREDENTIALS;
        }
        if (exception.getCode() == CommonApiCode.AUTH_ACCOUNT_DISABLED) {
            return FailureReason.ACCOUNT_DISABLED;
        }
        if (exception.getCode() == CommonApiCode.AUTH_REQUIRED) {
            return FailureReason.AUTH_REQUIRED;
        }
        if (exception.getCode() == CommonApiCode.AUTH_FORBIDDEN) {
            return FailureReason.AUTH_FORBIDDEN;
        }
        if (exception.getCode() == CommonApiCode.VALIDATION_FAILED) {
            return FailureReason.VALIDATION_FAILED;
        }
        return FailureReason.INTERNAL_ERROR;
    }

    private void recordAudit(AuthenticationAuditEvent event) {
        try {
            authenticationAuditRecorder.record(event);
        } catch (RuntimeException exception) {
            LOGGER.error("Authentication audit recorder failed and was ignored: eventType={} result={} failureType={}",
                    event.eventType(), event.result(), exception.getClass().getSimpleName());
        }
    }
}
