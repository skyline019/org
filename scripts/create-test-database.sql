-- 测试库（集成测试在无 Docker 时使用本地 MySQL）
-- 必须以 root 执行（CREATE DATABASE / GRANT 需要管理员权限）
-- GitHub Actions CI 使用 root/root；本地请替换为你的 root 密码

CREATE DATABASE IF NOT EXISTS org_auth_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON org_auth_test.* TO 'Voyager'@'localhost';
GRANT ALL PRIVILEGES ON org_auth_test.* TO 'Voyager'@'%';

FLUSH PRIVILEGES;
