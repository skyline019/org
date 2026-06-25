CREATE TABLE IF NOT EXISTS auth_audit_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type  VARCHAR(50)  NOT NULL,
    subject     VARCHAR(255) NULL,
    client_ip   VARCHAR(45)  NULL,
    detail      VARCHAR(500) NULL,
    occurred_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_audit_event_time ON auth_audit_events (event_type, occurred_at);
CREATE INDEX idx_auth_audit_subject_time ON auth_audit_events (subject, occurred_at);
