-- 测试库（集成测试在无 Docker 时使用本地 MySQL）
-- 必须以 root 执行（CREATE DATABASE / CREATE USER / GRANT 需要管理员权限）
-- 本地密码请与 application-test.properties / DB_PASSWORD 一致（默认示例 testpass）

CREATE DATABASE IF NOT EXISTS org_auth_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- MySQL 8 不允许 GRANT 隐式建用户，须先 CREATE USER
CREATE USER IF NOT EXISTS 'Voyager'@'localhost' IDENTIFIED BY 'testpass';
CREATE USER IF NOT EXISTS 'Voyager'@'%'         IDENTIFIED BY 'testpass';

GRANT ALL PRIVILEGES ON org_auth_test.* TO 'Voyager'@'localhost';
GRANT ALL PRIVILEGES ON org_auth_test.* TO 'Voyager'@'%';

FLUSH PRIVILEGES;
