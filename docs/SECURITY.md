# Org Auth — 安全说明

## 1. 威胁模型（简要）

| 威胁 | 控制措施 |
|------|----------|
| 暴力破解登录 | 账户锁定 + 登录限流 + 审计日志 |
| 用户枚举 | prod `enumeration-safe`；找回密码静默响应 |
| CSRF | 表单 CSRF Token；API check 只读 GET 豁免 |
| Session 劫持 | HttpOnly Cookie、fixation 防护、JDBC Session |
| Token 泄露 | 路径 Token + POST 确认；库内 SHA-256 哈希 |
| 邮件链接泄露 | Referrer-Policy: no-referrer |
| 接口滥用 | Bucket4j 限流（login/register/reset/resend/check） |
| 密码弱 | BCrypt(12) + 强度校验 |
| 横向扩展限流失效 | 可选 Redis 后端（`redis` profile） |

## 2. 已实施控制

### 认证与 Session

- BCrypt strength **12**
- JDBC Session，Cookie：`http-only`、`same-site=lax`；prod 下 `secure=true`
- 未验证邮箱：`DisabledException`，不计入失败次数
- 已锁定：`LockedException`，显示剩余分钟数

### Token 生命周期

- 邮箱验证：默认 24h；密码重置：默认 30min
- 发新 Token 时作废旧 Token（`invalidateActiveByUserId`）
- 使用后标记 `used=true`

### HTTP 安全头

- CSP（允许 inline style 以支持 Thymeleaf）
- X-Frame-Options: DENY
- Referrer-Policy: no-referrer

### 审计

- Logger 名：`AUTH_AUDIT`
- 事件：LOGIN_SUCCESS/FAILURE、REGISTER、EMAIL_VERIFIED、RESEND_VERIFICATION、PASSWORD_RESET_*、ACCOUNT_LOCKED、RATE_LIMITED
- **Micrometer Counter**：`auth.audit.events`（标签 `event`），与日志双写，详见 [OPERATIONS.md](OPERATIONS.md)

## 3. dev 与 prod 差异

| 项 | dev | prod |
|----|-----|------|
| check API 枚举 | 暴露占用状态 | 仅格式有效 |
| Cookie Secure | false | true |
| 错误详情 | 可见 | `include-message=never` |
| Forward headers | 未启用 | `framework` |
| Redis 健康检查 | 禁用 | 按需启用 |
| 日志级别 | DEBUG（包） | WARN root |

## 4. 部署检查清单

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `APP_BASE_URL` 为外网 HTTPS 地址
- [ ] 数据库与 SMTP 凭据来自环境变量
- [ ] `app.auth.check.enumeration-safe=true`
- [ ] 反向代理正确传递 `X-Forwarded-For` / `X-Forwarded-Proto`
- [ ] 多实例时启用 `redis` profile 或接受单机限流
- [ ] 配置 Prometheus / 日志采集监控 `AUTH_AUDIT` 与 `auth_audit_events_total`

## 5. 已知限制（非 bug）

- 无 CAPTCHA / MFA / OAuth2（扩展见 EXTENSION.md）
- 审计仅 SLF4J，未落库
- dev 下 check API 可枚举用户名/邮箱（便于开发调试）
