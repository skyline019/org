-- =============================================================================
-- Org 登录注册模块 — 数据库创建与初始化脚本
-- 适用：MySQL 8.x
-- =============================================================================
--
-- 使用方式（二选一）：
--
-- A) 仅建库建用户（推荐，表结构由 Flyway 自动迁移）
--    以 root 执行「第一部分」，然后启动应用：spring.flyway 会执行 db/migration/*.sql
--
-- B) 完全手动初始化（不依赖 Flyway 首次建表）
--    1. root 执行「第一部分」
--    2. 应用用户执行「第二部分」全文
--    3. 应用配置：spring.flyway.baseline-on-migrate=true（或 baseline-version=2）
--
-- =============================================================================

-- =============================================================================
-- 第一部分：以 root / 管理员 执行
-- =============================================================================

CREATE DATABASE IF NOT EXISTS org_auth
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 按需修改用户名与密码
CREATE USER IF NOT EXISTS 'Voyager'@'localhost' IDENTIFIED BY 'your_password_here';
CREATE USER IF NOT EXISTS 'Voyager'@'%'         IDENTIFIED BY 'your_password_here';

GRANT ALL PRIVILEGES ON org_auth.* TO 'Voyager'@'localhost';
GRANT ALL PRIVILEGES ON org_auth.* TO 'Voyager'@'%';

FLUSH PRIVILEGES;

-- =============================================================================
-- 第二部分：表结构与初始数据（以应用用户连接 org_auth 后执行）
-- mysql -u Voyager -p org_auth < scripts/schema-init.sql  仅执行下方内容亦可
-- =============================================================================

USE org_auth;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 业务表
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS login_attempts;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS email_verification_tokens;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    username         VARCHAR(50)  NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    enabled          TINYINT(1)   NOT NULL DEFAULT 0,
    email_verified   TINYINT(1)   NOT NULL DEFAULT 0,
    locked           TINYINT(1)   NOT NULL DEFAULT 0,
    lock_until       TIMESTAMP    NULL DEFAULT NULL,
    failed_attempts  INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY fk_user_roles_role (role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE email_verification_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verification_token_hash (token_hash),
    KEY idx_email_verification_expires (expires_at),
    KEY fk_email_verification_user (user_id),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_token_hash (token_hash),
    KEY idx_password_reset_expires (expires_at),
    KEY fk_password_reset_user (user_id),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE login_attempts (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    ip_address   VARCHAR(45)  NOT NULL,
    success      TINYINT(1)   NOT NULL,
    attempted_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_login_attempts_username (username),
    KEY idx_login_attempts_attempted_at (attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Spring Session JDBC（Session 持久化）
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS SPRING_SESSION_ATTRIBUTES;
DROP TABLE IF EXISTS SPRING_SESSION;

CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36)     NOT NULL,
    SESSION_ID            CHAR(36)     NOT NULL,
    CREATION_TIME         BIGINT       NOT NULL,
    LAST_ACCESS_TIME      BIGINT       NOT NULL,
    MAX_INACTIVE_INTERVAL INT          NOT NULL,
    EXPIRY_TIME           BIGINT       NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100) NULL DEFAULT NULL,
    PRIMARY KEY (PRIMARY_ID),
    UNIQUE KEY SPRING_SESSION_IX1 (SESSION_ID),
    KEY SPRING_SESSION_IX2 (EXPIRY_TIME),
    KEY SPRING_SESSION_IX3 (PRINCIPAL_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BLOB         NOT NULL,
    PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 初始数据
-- ---------------------------------------------------------------------------

INSERT INTO roles (name) VALUES ('ROLE_USER')
    ON DUPLICATE KEY UPDATE name = name;

-- ---------------------------------------------------------------------------
-- （可选）Flyway 基线：若已手动建表，避免 Flyway 重复执行 V1/V2
-- 仅在使用「完全手动初始化」方式 B 时取消下方注释
-- ---------------------------------------------------------------------------

-- CREATE TABLE IF NOT EXISTS flyway_schema_history (
--     installed_rank INT           NOT NULL,
--     version        VARCHAR(50)    NULL,
--     description    VARCHAR(200)   NOT NULL,
--     type           VARCHAR(20)    NOT NULL,
--     script         VARCHAR(1000)  NOT NULL,
--     checksum       INT            NULL,
--     installed_by   VARCHAR(100)   NOT NULL,
--     installed_on   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     execution_time INT            NOT NULL,
--     success        TINYINT(1)     NOT NULL,
--     PRIMARY KEY (installed_rank)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
--
-- INSERT INTO flyway_schema_history
--     (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
-- VALUES
--     (1, '1', 'init auth schema', 'SQL', 'V1__init_auth_schema.sql', NULL, 'manual', 0, 1),
--     (2, '2', 'spring session', 'SQL', 'V2__spring_session.sql', NULL, 'manual', 0, 1);

-- =============================================================================
-- 验证
-- =============================================================================
-- SHOW TABLES;
-- SELECT * FROM roles;
