package com.skyline.org.common.web;

import com.skyline.org.auth.config.TrustedProxyProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves client IP for auditing and rate limiting.
 * Forwarded headers are honored only when the direct peer is a trusted proxy.
 */
@Component
public class ClientIpResolver {

    private final TrustedProxyProperties trustedProxyProperties;

    public ClientIpResolver(TrustedProxyProperties trustedProxyProperties) {
        this.trustedProxyProperties = trustedProxyProperties;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxyProperties.isEnabled() && isTrustedProxy(remoteAddr)) {
            String forwarded = firstForwardedFor(request);
            if (forwarded != null) {
                return forwarded;
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        for (String network : trustedProxyProperties.getTrustedNetworks()) {
            if (remoteAddr.equals(network) || remoteAddr.startsWith(network)) {
                return true;
            }
        }
        return false;
    }

    private static String firstForwardedFor(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }
        int comma = forwarded.indexOf(',');
        return (comma < 0 ? forwarded : forwarded.substring(0, comma)).trim();
    }
}
