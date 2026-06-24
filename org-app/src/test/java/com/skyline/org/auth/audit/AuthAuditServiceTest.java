package com.skyline.org.auth.audit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthAuditServiceTest {

    @Test
    void incrementsCounterOnLog() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(registry);
        service.log(AuthEventType.LOGIN_SUCCESS, "alice", "127.0.0.1", null);
        assertThat(registry.find("auth.audit.events").tag("event", "LOGIN_SUCCESS").counter().count()).isEqualTo(1.0);
    }
}
