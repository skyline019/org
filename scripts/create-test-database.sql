-- 测试库（集成测试在无 Docker 时使用本地 MySQL）
-- 以 root 执行

CREATE DATABASE IF NOT EXISTS org_auth_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON org_auth_test.* TO 'Voyager'@'localhost';
GRANT ALL PRIVILEGES ON org_auth_test.* TO 'Voyager'@'%';

FLUSH PRIVILEGES;
