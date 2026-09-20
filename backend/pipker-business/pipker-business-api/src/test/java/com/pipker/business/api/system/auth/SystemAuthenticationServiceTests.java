package com.pipker.business.api.system.auth;

import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.system.auth.dto.LoginRequest;
import com.pipker.business.api.system.auth.dto.SystemLoginTypes;
import com.pipker.business.api.system.user.SystemAccountService;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.EventType;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.FailureReason;
import com.pipker.business.common.auth.audit.AuthenticationAuditRecorder;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.satoken.service.AuthSessionService;
import com.pipker.starter.satoken.service.AuthToken;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemAuthenticationServiceTests {

    private SystemAccountService accountService;
    private SecurityCryptoService cryptoService;
    private AuthSessionService sessionService;
    private AuthenticationAuditRecorder auditRecorder;
    private SystemAuthenticationService service;

    @BeforeEach
    void setUp() {
        accountService = mock(SystemAccountService.class);
        cryptoService = mock(SecurityCryptoService.class);
        sessionService = mock(AuthSessionService.class);
        auditRecorder = mock(AuthenticationAuditRecorder.class);
        service = new SystemAuthenticationService(accountService, cryptoService, sessionService, auditRecorder);
    }

    @Test
    void successfulLoginUpdatesLastLoginAndPublishesSafeSuccessEvent() {
        SystemUser user = enabledUser();
        when(accountService.findByUsername("admin")).thenReturn(user);
        when(cryptoService.matchesPassword("secret", user.getPasswordHash())).thenReturn(true);
        when(sessionService.login(any())).thenReturn(new AuthToken(
                "raw-token",
                new LoginIdentity(SystemLoginTypes.SYSTEM, String.valueOf(user.getId()))
        ));

        var response = service.login(new LoginRequest(" admin ", "secret"));

        assertThat(response.accessToken()).isEqualTo("raw-token");
        verify(accountService).recordSuccessfulLogin(any(Long.class), any());
        ArgumentCaptor<AuthenticationAuditEvent> event = ArgumentCaptor.forClass(AuthenticationAuditEvent.class);
        verify(auditRecorder).record(event.capture());
        assertThat(event.getValue()).satisfies(record -> {
            assertThat(record.eventType()).isEqualTo(EventType.LOGIN);
            assertThat(record.result()).isEqualTo(AuthenticationAuditEvent.Result.SUCCESS);
            assertThat(record.userId()).isEqualTo(user.getId());
            assertThat(record.username()).isEqualTo("admin");
            assertThat(record.toString()).doesNotContain("secret", "raw-token");
        });
    }

    @Test
    void invalidCredentialsAndDisabledAccountsPublishStableFailureReasons() {
        when(accountService.findByUsername("missing")).thenReturn(null);
        assertThatThrownBy(() -> service.login(new LoginRequest("missing", "secret")))
                .isInstanceOf(ApiBusinessException.class);
        verify(auditRecorder).record(AuthenticationAuditEvent.failure(
                EventType.LOGIN, FailureReason.INVALID_CREDENTIALS, null, "missing"
        ));

        auditRecorder = mock(AuthenticationAuditRecorder.class);
        service = new SystemAuthenticationService(accountService, cryptoService, sessionService, auditRecorder);
        SystemUser disabled = enabledUser();
        disabled.setStatus("DISABLED");
        when(accountService.findByUsername("admin")).thenReturn(disabled);
        when(cryptoService.matchesPassword("secret", disabled.getPasswordHash())).thenReturn(true);
        assertThatThrownBy(() -> service.login(new LoginRequest("admin", "secret")))
                .isInstanceOf(ApiBusinessException.class);
        verify(auditRecorder).record(AuthenticationAuditEvent.failure(
                EventType.LOGIN, FailureReason.ACCOUNT_DISABLED, disabled.getId(), "admin"
        ));
    }

    @Test
    void logoutInvalidatesOnlyCurrentSessionAndAuditFailureNeverBlocksIt() {
        SystemUser user = enabledUser();
        LoginIdentity identity = new LoginIdentity(SystemLoginTypes.SYSTEM, String.valueOf(user.getId()));
        when(sessionService.currentIdentity()).thenReturn(Optional.of(identity));
        when(accountService.findById(user.getId())).thenReturn(user);
        doThrow(new IllegalStateException("audit unavailable")).when(auditRecorder).record(any());

        assertThatCode(() -> assertThat(service.logoutCurrent().loggedOut()).isTrue()).doesNotThrowAnyException();
        verify(sessionService).logoutCurrent();
    }

    private SystemUser enabledUser() {
        SystemUser user = new SystemUser();
        user.setId(2090000000000000001L);
        user.setUsername("admin");
        user.setPasswordHash("{bcrypt}hash");
        user.setStatus("ENABLED");
        return user;
    }
}
