# Security operations guide

## Credential rotation

If database or SMTP credentials may have appeared in git history, local backups, or shared configs:

1. **Rotate MySQL password** for the application user and update `DB_PASSWORD` / `application-dev.local.properties`.
2. **Rotate SMTP / app passwords** and OAuth client secrets.
3. **Invalidate active sessions** if a session store compromise is suspected (`DELETE FROM SPRING_SESSION` in maintenance window).
4. **Revoke OAuth tokens** at the identity provider and regenerate client secrets.

Never commit `application-dev.local.properties`, `.env`, or real passwords in SQL scripts. Use `your_password_here` placeholders in `scripts/create-database.sql`.

## Production checklist

| Item | Configuration |
|------|----------------|
| User enumeration | `app.auth.check.enumeration-safe=true` |
| Distributed rate limit | `spring.profiles.active=prod` (activates `redis` via profile group), `app.auth.rate-limit.backend=redis` |
| Trusted proxy IP | `app.trusted-proxy.enabled=true` behind nginx/ALB |
| Forwarded headers | `server.forward-headers-strategy=framework` |
| Secure session cookie | `server.servlet.session.cookie.secure=true` |
| HSTS | Enabled on `prod` profile (`max-age=31536000; includeSubDomains`) |
| Session idle timeout | `server.servlet.session.timeout=30m` |
| CSP | Tight `default-src 'self'` (no unused CDN hosts) |
| Swagger / OpenAPI | `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false` (SecurityConfig also requires `ROLE_ADMIN` if re-enabled) |
| Bootstrap admin | **Disabled in prod** (`@Profile("!prod")`); assign `ROLE_ADMIN` via SQL instead |

## Account lockout

| Setting | Default |
|---------|---------|
| `app.auth.lock.max-attempts` | 5 |
| `app.auth.lock.duration` | 15m |

Lockout is per username. Expired locks auto-clear on next login attempt.

## Rate limiting

| Path | Default limit/min |
|------|-------------------|
| POST `/login` | 10 |
| GET `/oauth2/**`, `/login/oauth2/**` | 10 (same as login) |
| POST register / reset / resend | 5–5–3 |
| GET check API | 30 |
| POST `/admin/**` | 30 |

Memory and Redis backends both use token-bucket semantics. HTML form posts receive a redirect to `/login?rateLimited`; API clients receive HTTP 429 JSON.

## Session policy

- Session fixation: `changeSessionId()`
- Concurrent sessions: max **3** per user (oldest evicted); enforced via `SpringSessionBackedSessionRegistry`
- Idle timeout: **30 minutes** (`server.servlet.session.timeout=30m`)
- Cookie: `HttpOnly`, `SameSite=Lax` (dev/test); `Secure` in prod

## CSRF

Enabled globally. Exempt: `GET /api/v1/auth/check/**` only.

## Trusted proxy

When `app.trusted-proxy.enabled=false` (default dev), `X-Forwarded-For` is **ignored** and `remoteAddr` is used — clients cannot spoof IP for rate limiting.

When `true`, forwarded headers are honored only if the direct TCP peer matches `app.trusted-proxy.trusted-networks` (defaults: loopback + RFC1918).

## Bootstrap admin (non-production)

Only active when profile is **not** `prod` and:

```properties
app.auth.bootstrap-admin.enabled=true
app.auth.bootstrap-admin.username=admin
app.auth.bootstrap-admin.email=admin@example.com
app.auth.bootstrap-admin.password=<strong-password>
```

Disable after first boot. Prefer assigning `ROLE_ADMIN` via SQL in production.

## OAuth2

Enable optional social login (use profile group or activate `oauth2` alongside `prod`):

```properties
spring.profiles.active=prod,oauth2
app.auth.oauth2.enabled=true
```

Copy `application-oauth2.properties.example` and set provider client IDs/secrets via environment variables.

**Linking rules:**
- Provider must return a verified email; synthetic `@oauth.local` addresses are not created.
- OAuth links to an existing local account **only if** that account's email is already verified.
- Unverified local registrations cannot be taken over via OAuth.
- Disabled accounts cannot complete OAuth login.

OAuth accounts are stored in `oauth_accounts`.

## Multi-factor authentication (TOTP)

Optional step-up authentication after password/OAuth login:

```properties
app.auth.mfa.enabled=true
app.auth.mfa.issuer=Your App Name
```

Users enroll at `/auth/mfa/setup` (QR code + 6-digit TOTP). After enrollment, login redirects to `/auth/mfa/challenge` until the session attribute `MFA_VERIFIED` is set. Secrets are stored in `user_totp_credentials` (Flyway V5); one-time recovery codes in `user_mfa_recovery_codes` (V6).

```properties
app.auth.mfa.enforce-for-roles=ROLE_ADMIN
app.auth.mfa.recovery-code-count=10
```

`enforce-for-roles` requires listed roles to complete MFA setup before accessing the app. Recovery codes can be used once on the challenge page if the authenticator is unavailable.

Audit events: `MFA_ENROLLED`, `MFA_CHALLENGE_SUCCESS`, `MFA_CHALLENGE_FAILURE`. `LOGIN_SUCCESS` is deferred until MFA passes when MFA is required.

## Admin console

- URL: `/admin/users` (requires `ROLE_ADMIN`)
- Cannot disable or revoke admin role from the **last** admin account
- Admin POST actions are CSRF-protected and rate-limited

## Audit

Security events are written to:

1. Logger **`AUTH_AUDIT`** (always)
2. Micrometer counter `auth.audit.events` (always)
3. Table **`auth_audit_events`** when `app.auth.audit.persist=true` (enabled in prod; writes are **async** via `auditTaskExecutor`)

| Setting | Default (dev) | Production |
|---------|---------------|------------|
| `app.auth.audit.persist` | `false` | `true` |
| `app.auth.audit.retention` | `90d` | `90d` |

Stale rows are purged nightly by `AuthMaintenanceScheduler`. Forward `AUTH_AUDIT` logs to your SIEM for real-time alerting.
