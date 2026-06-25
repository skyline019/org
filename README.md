# Org — 登录注册安全模块

基于 Spring Boot 4.1 的模块化登录注册系统：Thymeleaf 页面 + REST 动态校验、MySQL 持久化、SMTP 邮件验证与密码找回。

## 功能

- 用户注册（用户名/邮箱/密码实时校验）
- 邮箱验证激活（路径 Token + POST 确认，避免查询参数泄露）
- 重发验证邮件（限流保护）
- 表单登录（Session + JDBC 存储）
- 账户锁定（5 次失败锁定 15 分钟）
- 忘记密码 / 重置密码
- 接口限流（内存或 Redis 后端）
- 安全审计日志（`AUTH_AUDIT`）+ Micrometer Counter
- OpenAPI / Swagger UI
- Prometheus 指标（prod）
- 完整测试 + JaCoCo 覆盖率 + GitHub Actions CI
- 可选 OAuth2 社交登录（GitHub / Google OIDC）
- 可选 TOTP 双因素认证（MFA）
- 管理员用户控制台（`/admin/users`）
- 生产安全基线：CSP 收紧、可信代理 IP、Redis 分布式限流（见 [docs/SECURITY.md](docs/SECURITY.md)）

## 前置条件

1. **JDK 21+**
2. **MySQL 8.x**
3. **SMTP 邮件服务**

## 配置

复制 `org-app/src/main/resources/application-dev.local.properties.example` 为同目录下的 `application-dev.local.properties` 填写数据库与 SMTP（该文件已 gitignore，勿提交）。`scripts/create-database.sql` 中的 `your_password_here` 也需替换为实际密码后再执行。

| 变量 | 说明 |
|------|------|
| `DB_USERNAME` / `DB_PASSWORD` | 数据库凭据 |
| `MAIL_*` | SMTP 配置 |
| `APP_BASE_URL` | 邮件链接外网地址 |
| `REDIS_HOST` | 可选，启用 Redis 限流时使用 profile `redis`（见 `application-redis.properties`） |

## 启动

默认运行 `org-app` 模块：

```bash
.\mvnw.cmd -pl org-app spring-boot:run -Dspring-boot.run.profiles=dev
```

嵌入业务系统时，引入 `org-auth-starter` 依赖即可自动装配（见 `demo-business` 示例）。运行前同样复制 `application-dev.local.properties.example` 为 `application-dev.local.properties`。

```bash
.\mvnw.cmd -pl demo-business spring-boot:run
```

访问：

- 登录：http://localhost:8080/login
- 注册：http://localhost:8080/register
- API 文档：http://localhost:8080/swagger-ui.html

## API（v1）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/auth/check/username?value=` | 用户名校验 |
| GET | `/api/v1/auth/check/email?value=` | 邮箱校验 |
| GET | `/api/v1/auth/check/password?value=` | 密码强度 |
| POST | `/api/v1/auth/register` | JSON 注册（需 CSRF） |
| POST | `/api/v1/auth/resend-verification` | 重发验证邮件 |

生产环境 `app.auth.check.enumeration-safe=true` 时，check 接口不暴露用户名/邮箱是否已注册。

## 测试

```bash
mysql -u root -p < scripts/create-test-database.sql
.\mvnw.cmd verify
```

JaCoCo 聚合报告：`org-coverage/target/site/jacoco-aggregate/index.html`（根目录 `verify` 要求 `com.skyline.org` 指令覆盖率 ≥ 80%、分支覆盖率 ≥ 65%，含 SpotBugs 静态分析与 OWASP dependency-check；与 CI 一致）

**CI 测试环境**：GitHub Actions 通过 `docker-compose.ci.yml` 启动 MySQL + Redis（仅 CI 使用，本地开发无需 Docker）。

E2E（Playwright）需单独启用 profile：

```bash
.\mvnw.cmd -pl org-app -am test -Pe2e
```

## 模块结构

| 模块 | 说明 |
|------|------|
| `org-auth-core` | 领域模型、服务、仓储、Flyway 迁移 |
| `org-auth-web` | Controller、Security、Thymeleaf 模板 |
| `org-auth-starter` | Spring Boot 自动配置 |
| `org-app` | 可运行应用与全部测试 |
| `demo-business` | 嵌入示例（`/dashboard` + `OrgAuthSecurityCustomizer`） |
| `org-coverage` | JaCoCo 聚合报告与覆盖率门禁（指令 ≥80%、分支 ≥65%） |

## Maven 发布

父 POM 已配置 GitHub Packages（`distributionManagement`）。在 `~/.m2/settings.xml` 中配置 PAT：

```xml
<servers>
  <server>
    <id>github</id>
    <username>skyline019</username>
    <password>YOUR_GITHUB_PAT</password>
  </server>
</servers>
```

发布 starter 及依赖模块：

```bash
.\mvnw.cmd deploy -pl org-auth-starter -am -DskipTests
```

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [安全设计](docs/SECURITY.md)
- [扩展指南](docs/EXTENSION.md)
- [运维与指标](docs/OPERATIONS.md)

## 生产部署

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://host:3306/org_auth
export APP_BASE_URL=https://your-domain.com
export REDIS_HOST=your-redis-host
export MFA_SECRET_ENCRYPTION_KEY=$(openssl rand -base64 32)
# prod 已默认通过 profile group 激活 redis 与 app.auth.rate-limit.backend=redis
```

启用反向代理时已配置 `server.forward-headers-strategy=framework`。
