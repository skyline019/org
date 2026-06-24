-- 仅建库与授权（表结构交给 Flyway）
-- 以 root 执行

CREATE DATABASE IF NOT EXISTS org_auth
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'Voyager'@'localhost' IDENTIFIED BY '760722zH@';
CREATE USER IF NOT EXISTS 'Voyager'@'%'         IDENTIFIED BY '760722zH@';

GRANT ALL PRIVILEGES ON org_auth.* TO 'Voyager'@'localhost';
GRANT ALL PRIVILEGES ON org_auth.* TO 'Voyager'@'%';

FLUSH PRIVILEGES;
