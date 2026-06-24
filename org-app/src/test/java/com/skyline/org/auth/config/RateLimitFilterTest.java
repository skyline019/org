package com.skyline.org.auth.config;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.ratelimit.RateLimitService;
import com.skyline.org.common.i18n.Messages;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    void returns429WhenLimited() throws Exception {
        RateLimitService rateLimitService = mock(RateLimitService.class);
        AuthAuditService audit = mock(AuthAuditService.class);
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.rate-limit", Locale.getDefault(), "slow down");
        Messages messages = new Messages(source);
        when(rateLimitService.tryConsume(any(), any(), any())).thenReturn(false);

        RateLimitFilter filter = new RateLimitFilter(rateLimitService, audit, messages);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());
    }
}
