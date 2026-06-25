package com.skyline.org.auth.config;

import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.MfaSessionKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.auth.mfa.enabled", havingValue = "true")
public class MfaEnforcementFilter extends OncePerRequestFilter {

    private final MfaService mfaService;

    public MfaEnforcementFilter(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!mfaService.isFeatureEnabled() || isExemptPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = authentication.getName();
        if (mfaService.requiresMandatoryEnrollment(username, authentication.getAuthorities())) {
            response.sendRedirect(request.getContextPath() + "/auth/mfa/setup");
            return;
        }

        if (!mfaService.requiresChallenge(username)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(MfaSessionKeys.MFA_VERIFIED))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/auth/mfa/challenge");
    }

    private boolean isExemptPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/auth/mfa")
                || path.startsWith("/login")
                || path.startsWith("/logout")
                || path.startsWith("/oauth2/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/error")
                || path.startsWith("/actuator/");
    }
}
