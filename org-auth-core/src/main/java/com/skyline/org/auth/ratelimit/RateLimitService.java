package com.skyline.org.auth.ratelimit;

import com.skyline.org.auth.config.AuthProperties;
import org.springframework.stereotype.Service;

/**
 * Token-bucket rate limiter delegating to memory or Redis backend.
 */
@Service
public class RateLimitService {

    private final AuthProperties authProperties;
    private final RateLimitBackend backend;

    public RateLimitService(AuthProperties authProperties, RateLimitBackend backend) {
        this.authProperties = authProperties;
        this.backend = backend;
    }

    public boolean tryConsume(String path, String method, String clientIp) {
        Integer limit = resolveLimit(path, method);
        if (limit == null) {
            return true;
        }
        String key = path + ':' + clientIp;
        return backend.tryConsume(key, limit);
    }

    private Integer resolveLimit(String path, String method) {
        if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            return authProperties.getAuth().getRateLimit().getLoginPerMinute();
        }
        if (("/register".equals(path) || path.startsWith("/api/v1/auth/register"))
                && "POST".equalsIgnoreCase(method)) {
            return authProperties.getAuth().getRateLimit().getRegisterPerMinute();
        }
        if (path.startsWith("/auth/forgot-password") && "POST".equalsIgnoreCase(method)) {
            return authProperties.getAuth().getRateLimit().getResetPerMinute();
        }
        if ("/auth/reset-password".equals(path) && "POST".equalsIgnoreCase(method)) {
            return authProperties.getAuth().getRateLimit().getResetPerMinute();
        }
        if ((path.startsWith("/auth/resend-verification") || path.startsWith("/api/v1/auth/resend-verification"))
                && "POST".equalsIgnoreCase(method)) {
            return authProperties.getAuth().getRateLimit().getResendPerMinute();
        }
        if (path.startsWith("/api/v1/auth/check/") && "GET".equalsIgnoreCase(method)) {
            return authProperties.getAuth().getRateLimit().getCheckPerMinute();
        }
        return null;
    }
}
