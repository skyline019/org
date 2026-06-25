# Performance baseline

## AuthLoadBaselineTest

`org-app` includes a lightweight concurrency test (`AuthLoadBaselineTest`) that exercises:

- **Endpoint:** `GET /api/v1/auth/check/username`
- **Load:** 20 threads × 10 requests (200 samples)
- **Gate:** p95 latency **< 800 ms** on CI hardware (GitHub Actions MySQL service)

This is **not** a substitute for JMeter/Gatling at production scale. It guards against accidental regressions (missing indexes, synchronous blocking, etc.).

## Recommended production load test

Before go-live, run a dedicated tool against a staging environment:

```bash
# Example with hey (install separately)
hey -n 2000 -c 50 "https://staging.example.com/api/v1/auth/check/username?value=demo"
```

Record p50/p95/p99 and error rate. Tune HikariCP pool, Redis rate-limit backend, and BCrypt cost accordingly.

## Current design notes

| Area | Behavior |
|------|----------|
| Password hashing | BCrypt(12) outside DB transactions |
| Availability check | Caffeine cache TTL 5m |
| Rate limit | memory (dev) / Redis (prod profile) |
| Mail | Async executor |

See also [SECURITY.md](SECURITY.md) for production hardening.
