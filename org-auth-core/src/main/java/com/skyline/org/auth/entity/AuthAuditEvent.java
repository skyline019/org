package com.skyline.org.auth.entity;

import com.skyline.org.auth.audit.AuthEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "auth_audit_events")
public class AuthAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuthEventType eventType;

    @Column(length = 255)
    private String subject;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(length = 500)
    private String detail;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuthAuditEvent() {
    }

    public AuthAuditEvent(AuthEventType eventType, String subject, String clientIp, String detail) {
        this.eventType = eventType;
        this.subject = normalize(subject);
        this.clientIp = normalize(clientIp);
        this.detail = normalize(detail);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public AuthEventType getEventType() {
        return eventType;
    }

    public String getSubject() {
        return subject;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
