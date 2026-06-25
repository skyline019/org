package com.skyline.org.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.ratelimit.RateLimitService;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.response.ApiResponse;
import com.skyline.org.common.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final RateLimitService rateLimitService;
    private final AuthAuditService authAuditService;
    private final Messages messages;

    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            AuthAuditService authAuditService,
            Messages messages,
            ClientIpResolver clientIpResolver) {
        this.rateLimitService = rateLimitService;
        this.authAuditService = authAuditService;
        this.messages = messages;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String clientIp = clientIpResolver.resolve(request);

        if (!rateLimitService.tryConsume(path, request.getMethod(), clientIp)) {
            authAuditService.log(AuthEventType.RATE_LIMITED, null, clientIp, path);
            if (prefersHtmlResponse(request)) {
                response.sendRedirect(request.getContextPath() + "/login?rateLimited");
                return;
            }
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> body = ApiResponse.fail(ErrorCode.RATE_LIMIT_EXCEEDED.name(), messages.get("auth.rate-limit"));
            OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean prefersHtmlResponse(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }
}
