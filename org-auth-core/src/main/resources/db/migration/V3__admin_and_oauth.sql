-- Idempotent: safe to re-run after a previously failed partial migration.
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN');

CREATE TABLE IF NOT EXISTS oauth_accounts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(50)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_oauth_user_id (user_id)
);

-- Backfill index when table existed from an earlier partial run without the inline index.
SET @missing_oauth_user_idx = (
    SELECT COUNT(*) = 0
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'oauth_accounts'
      AND index_name = 'idx_oauth_user_id'
);
SET @create_oauth_user_idx = IF(
    @missing_oauth_user_idx,
    'CREATE INDEX idx_oauth_user_id ON oauth_accounts (user_id)',
    'SELECT 1'
);
PREPARE oauth_user_idx_stmt FROM @create_oauth_user_idx;
EXECUTE oauth_user_idx_stmt;
DEALLOCATE PREPARE oauth_user_idx_stmt;
