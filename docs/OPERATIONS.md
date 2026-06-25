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

> Spring Boot 默认将 Counter 名 `auth.audit.events` 规范化为 `auth_audit_events_total`（取决于 registry 命名策略）。

### 2.2 示例 PromQL

```promql
# 5 分钟内登录失败速率
rate(auth_audit_events_total{event="LOGIN_FAILURE"}[5m])

# 限流触发次数
increase(auth_audit_events_total{event="RATE_LIMITED"}[1h])

# 注册成功数
increase(auth_audit_events_total{event="REGISTER"}[24h])
```

### 2.3 审计表查询（`auth_audit_events`）

生产环境 `app.auth.audit.persist=true` 时，事件同步写入 MySQL：

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

## 5. 质量门禁（CI）

`mvn verify` 包含：

- 全量单元/集成测试
- JaCoCo 指令覆盖率 ≥ **75%**
- SpotBugs 静态分析（Medium 及以上失败；Spring/JPA 常见误报见 `config/spotbugs-exclude.xml`）
