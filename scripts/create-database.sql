-- 仅建库与授权（表结构交给 Flyway）
-- 以 root 执行；请将 your_password_here 替换为实际密码（勿提交到 Git）

CREATE DATABASE IF NOT EXISTS org_auth
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'Voyager'@'localhost' IDENTIFIED BY 'your_password_here';
CREATE USER IF NOT EXISTS 'Voyager'@'%'         IDENTIFIED BY 'your_password_here';

GRANT ALL PRIVILEGES ON org_auth.* TO 'Voyager'@'localhost';
GRANT ALL PRIVILEGES ON org_auth.* TO 'Voyager'@'%';

FLUSH PRIVILEGES;
