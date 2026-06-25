CREATE TABLE IF NOT EXISTS user_mfa_recovery_codes (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    code_hash   VARCHAR(100) NOT NULL,
    used_at     TIMESTAMP    NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mfa_recovery_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_mfa_recovery_user_unused (user_id, used_at)
);
