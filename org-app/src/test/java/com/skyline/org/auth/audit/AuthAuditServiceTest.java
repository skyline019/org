package com.skyline.org.auth.audit;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.AuthAuditEvent;
import com.skyline.org.auth.repository.AuthAuditEventRepository;
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

    @Mock AuthAuditEventRepository auditEventRepository;

    @Test
    void incrementsCounterOnLog() {
        AuthProperties properties = new AuthProperties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(properties, auditEventRepository, registry);
        service.log(AuthEventType.LOGIN_SUCCESS, "alice", "127.0.0.1", null);
        assertThat(registry.find("auth.audit.events").tag("event", "LOGIN_SUCCESS").counter().count()).isEqualTo(1.0);
        verifyNoInteractions(auditEventRepository);
    }

    @Test
    void persistsWhenEnabled() {
        AuthProperties properties = new AuthProperties();
        properties.getAuth().getAudit().setPersist(true);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(properties, auditEventRepository, registry);

        service.log(AuthEventType.LOGIN_FAILURE, "bob", "203.0.113.1", "BadCredentials");

        ArgumentCaptor<AuthAuditEvent> captor = ArgumentCaptor.forClass(AuthAuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuthAuditEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuthEventType.LOGIN_FAILURE);
        assertThat(saved.getSubject()).isEqualTo("bob");
        assertThat(saved.getClientIp()).isEqualTo("203.0.113.1");
        assertThat(saved.getDetail()).isEqualTo("BadCredentials");
    }
}
