ALTER TABLE user_totp_credentials
    MODIFY COLUMN secret VARCHAR(512) NOT NULL;
