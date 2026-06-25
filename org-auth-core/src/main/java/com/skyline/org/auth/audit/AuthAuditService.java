package com.skyline.org.auth.audit;

import com.skyline.org.auth.config.AuthProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class AuthAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUTH_AUDIT");

    private final AuthProperties authProperties;
    private final AuthAuditPersistenceService auditPersistenceService;
    private final Map<AuthEventType, Counter> eventCounters;

    public AuthAuditService(
            AuthProperties authProperties,
            AuthAuditPersistenceService auditPersistenceService,
            MeterRegistry meterRegistry) {
        this.authProperties = authProperties;
        this.auditPersistenceService = auditPersistenceService;
        Map<AuthEventType, Counter> counters = new EnumMap<>(AuthEventType.class);
        for (AuthEventType type : AuthEventType.values()) {
            counters.put(type, Counter.builder("auth.audit.events")
                    .description("Auth security audit events")
                    .tag("event", type.name())
                    .register(meterRegistry));
        }
        this.eventCounters = Map.copyOf(counters);
    }

    public void log(AuthEventType event, String subject, String clientIp, String detail) {
        auditLog.info("event={} subject={} ip={} detail={}",
                event.name(), nullToDash(subject), nullToDash(clientIp), nullToDash(detail));
        eventCounters.get(event).increment();
        if (authProperties.getAuth().getAudit().isPersist()) {
            auditPersistenceService.persist(event, subject, clientIp, detail);
        }
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
