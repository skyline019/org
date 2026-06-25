package com.skyline.org.auth.audit;

import com.skyline.org.auth.config.AuthProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthAuditServiceTest {

    @Mock AuthAuditPersistenceService auditPersistenceService;

    @Test
    void persistsWhenEnabled() {
        AuthProperties properties = new AuthProperties();
        properties.getAuth().getAudit().setPersist(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(properties, auditPersistenceService, registry);

        service.log(AuthEventType.LOGIN_FAILURE, "bob", "203.0.113.1", "BadCredentials");

        ArgumentCaptor<AuthEventType> eventCaptor = ArgumentCaptor.forClass(AuthEventType.class);
        verify(auditPersistenceService).persist(
                eventCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("bob"),
                org.mockito.ArgumentMatchers.eq("203.0.113.1"),
                org.mockito.ArgumentMatchers.eq("BadCredentials"));
        assertThat(eventCaptor.getValue()).isEqualTo(AuthEventType.LOGIN_FAILURE);
    }

    @Test
    void incrementsCounterOnLog() {
        AuthProperties properties = new AuthProperties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(properties, auditPersistenceService, registry);
        service.log(AuthEventType.LOGIN_SUCCESS, "alice", "127.0.0.1", null);
        assertThat(registry.find("auth.audit.events").tag("event", "LOGIN_SUCCESS").counter().count()).isEqualTo(1.0);
        verifyNoInteractions(auditPersistenceService);
    }

    @Test
    void normalizesBlankAuditFields() {
        AuthProperties properties = new AuthProperties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(properties, auditPersistenceService, registry);

        service.log(AuthEventType.RATE_LIMITED, " ", null, "");

        assertThat(registry.find("auth.audit.events").tag("event", "RATE_LIMITED").counter().count()).isEqualTo(1.0);
        verifyNoInteractions(auditPersistenceService);
    }
}
