CREATE TABLE IF NOT EXISTS user_totp_credentials (
    user_id     BIGINT       NOT NULL PRIMARY KEY,
    secret      VARCHAR(64)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_totp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
