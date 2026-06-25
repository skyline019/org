# Org Auth — 运维与可观测性

## 1. 健康检查

| 端点 | 说明 |
|------|------|
| `/actuator/health` | 应用、数据库、邮件（prod） |
| `/actuator/info` | 构建信息（按需配置） |

## 2. Prometheus 指标

生产环境（`application-prod.properties`）暴露 `/actuator/prometheus`。

### 2.1 认证审计 Counter

每次 `AuthAuditService.log()` 同步递增 Micrometer Counter：

| 指标名 | 标签 | 说明 |
|--------|------|------|
| `auth_audit_events_total` | `event=<AuthEventType>` | 安全审计事件计数 |

`AuthEventType` 取值：

- `LOGIN_SUCCESS` / `LOGIN_FAILURE`
- `REGISTER` / `EMAIL_VERIFIED` / `RESEND_VERIFICATION`
- `PASSWORD_RESET_REQUEST` / `PASSWORD_RESET`
- `ACCOUNT_LOCKED` / `RATE_LIMITED`
- `ADMIN_USER_UPDATED`
- `MFA_ENROLLED` / `MFA_CHALLENGE_SUCCESS` / `MFA_CHALLENGE_FAILURE`

> Spring Boot 默认将 Counter 名 `auth.audit.events` 规范化为 `auth_audit_events_total`（取决于 registry 命名策略）。

### 2.2 示例 PromQL

```promql
# 5 分钟内登录失败速率
rate(auth_audit_events_total{event="LOGIN_FAILURE"}[5m])

# 限流触发次数
increase(auth_audit_events_total{event="RATE_LIMITED"}[1h])

# 注册成功数
increase(auth_audit_events_total{event="REGISTER"}[24h])

# MFA 挑战失败（可能的凭证攻击）
rate(auth_audit_events_total{event="MFA_CHALLENGE_FAILURE"}[5m])
```

### 2.3 审计表查询（`auth_audit_events`）

生产环境 `app.auth.audit.persist=true` 时，事件通过 `auditTaskExecutor` **异步**写入 MySQL（日志与 Counter 仍同步）：

```sql
-- 最近 1 小时登录失败
SELECT occurred_at, subject, client_ip, detail
FROM auth_audit_events
WHERE event_type = 'LOGIN_FAILURE'
  AND occurred_at > NOW() - INTERVAL 1 HOUR
ORDER BY occurred_at DESC;

-- 某用户的审计轨迹
SELECT event_type, occurred_at, client_ip, detail
FROM auth_audit_events
WHERE subject = 'alice'
ORDER BY occurred_at DESC
LIMIT 50;
```

### 2.4 建议告警

| 告警 | 条件 | 说明 |
|------|------|------|
| 登录失败突增 | `rate(...LOGIN_FAILURE...)` > 基线 3σ | 暴力破解 |
| 限流频繁 | `rate(...RATE_LIMITED...)` > 阈值 | 接口滥用 |
| 账户锁定增多 | `increase(...ACCOUNT_LOCKED...)` | 攻击或凭证问题 |
| MFA 挑战失败突增 | `rate(...MFA_CHALLENGE_FAILURE...)` > 基线 | TOTP/恢复码暴力尝试 |

## 3. 结构化审计日志

Logger 名称：`AUTH_AUDIT`（SLF4J）

格式：

```
event=LOGIN_FAILURE subject=alice ip=203.0.113.1 detail=BadCredentials
```

生产建议：

- 将 `AUTH_AUDIT` 路由至独立索引/Topic
- 与 Micrometer Counter 交叉验证（日志 + 指标双通道）

## 4. API 版本

REST 认证接口仅保留 **`/api/v1/auth/**`**。旧路径 `/api/auth/**` 已移除，未映射请求返回 404。

## 5. 生产环境变量（补充）

除 README 中的 `DB_*`、`MAIL_*`、`APP_BASE_URL`、`REDIS_HOST` 外，启用 MFA 与 secret 加密时需设置：

| 变量 | 说明 |
|------|------|
| `MFA_SECRET_ENCRYPTION_KEY` | Base64 编码的 32 字节 AES 密钥（prod 默认 `app.auth.mfa.secret-encryption.mode=local`） |
| `MFA_KMS_KEY_ID` | 可选，逻辑 KMS 密钥 ID（审计/未来云 KMS 集成；默认 `org-auth-totp`） |

生成示例：

```bash
openssl rand -base64 32
```

轮换密钥时需重新密封存量 TOTP secret（当前版本支持读取旧明文与 `v1:` 密封格式；跨密钥轮换需运维脚本，见 [SECURITY.md](SECURITY.md)）。

## 6. 质量门禁（CI）

GitHub Actions（`.github/workflows/ci.yml`）在 push/PR 时执行：

| 步骤 | 说明 |
|------|------|
| `docker compose -f docker-compose.ci.yml up` | MySQL 8 + Redis 7（仅 CI） |
| `./mvnw verify` | 单元 + 集成测试、JaCoCo、SpotBugs |
| `./mvnw -pl org-app -am test -Pe2e` | Playwright E2E（Chromium） |
| OWASP dependency-check | 独立 job，CVSS ≥ 7 失败 |
| CodeQL | 每周 + PR 静态分析（`.github/workflows/codeql.yml`） |

JaCoCo 聚合门禁（`org-coverage/pom.xml`，与 README 一致）：

- 指令覆盖率 ≥ **80%**
- 分支覆盖率 ≥ **65%**
- SpotBugs：Medium 及以上失败（Spring/JPA 常见误报见 `config/spotbugs-exclude.xml`）

报告路径：`org-coverage/target/site/jacoco-aggregate/index.html`

本地 E2E：

```bash
.\mvnw.cmd -pl org-app -am test -Pe2e
```
