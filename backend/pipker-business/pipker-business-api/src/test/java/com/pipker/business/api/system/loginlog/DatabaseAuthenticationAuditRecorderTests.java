package com.pipker.business.api.system.loginlog;

import com.pipker.business.api.common.model.SystemLoginLog;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.EventType;
import com.pipker.business.common.auth.audit.AuthenticationAuditEvent.FailureReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DatabaseAuthenticationAuditRecorderTests {

    @AfterEach
    void cleanContext() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void enrichesAndBoundsRequestMetadataWithoutRecordingSecrets() {
        SystemLoginLogWriter writer = mock(SystemLoginLogWriter.class);
        DatabaseAuthenticationAuditRecorder recorder = new DatabaseAuthenticationAuditRecorder(writer);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("2001:db8::1");
        request.addHeader("User-Agent", "x".repeat(600));
        request.addHeader("Authorization", "Bearer must-not-be-recorded");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("traceId", "trace-123");

        recorder.record(AuthenticationAuditEvent.failure(
                EventType.LOGIN, FailureReason.INVALID_CREDENTIALS, null, "attempted-user"
        ));

        ArgumentCaptor<SystemLoginLog> log = ArgumentCaptor.forClass(SystemLoginLog.class);
        verify(writer).write(log.capture());
        assertThat(log.getValue()).satisfies(record -> {
            assertThat(record.getUsername()).isEqualTo("attempted-user");
            assertThat(record.getEventType()).isEqualTo("LOGIN");
            assertThat(record.getFailureReason()).isEqualTo("INVALID_CREDENTIALS");
            assertThat(record.getClientIp()).isEqualTo("2001:db8::1");
            assertThat(record.getUserAgent()).hasSize(512);
            assertThat(record.getHttpMethod()).isEqualTo("POST");
            assertThat(record.getRequestPath()).isEqualTo("/api/auth/login");
            assertThat(record.getTraceId()).isEqualTo("trace-123");
            assertThat(record.toString()).doesNotContain("must-not-be-recorded");
        });
    }

    @Test
    void writerFailureIsContained() {
        SystemLoginLogWriter writer = mock(SystemLoginLogWriter.class);
        doThrow(new IllegalStateException("database unavailable")).when(writer).write(any());
        DatabaseAuthenticationAuditRecorder recorder = new DatabaseAuthenticationAuditRecorder(writer);

        assertThatCode(() -> recorder.record(AuthenticationAuditEvent.success(EventType.LOGOUT, 1L, "admin")))
                .doesNotThrowAnyException();
    }
}
