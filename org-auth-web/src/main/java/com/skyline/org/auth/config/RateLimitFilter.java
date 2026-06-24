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

    public RateLimitFilter(
            RateLimitService rateLimitService,
            AuthAuditService authAuditService,
            Messages messages) {
        this.rateLimitService = rateLimitService;
        this.authAuditService = authAuditService;
        this.messages = messages;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String clientIp = ClientIpResolver.resolve(request);

        if (!rateLimitService.tryConsume(path, request.getMethod(), clientIp)) {
            authAuditService.log(AuthEventType.RATE_LIMITED, null, clientIp, path);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiResponse<Void> body = ApiResponse.fail(ErrorCode.RATE_LIMIT_EXCEEDED.name(), messages.get("auth.rate-limit"));
            OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
