package com.skyline.org.auth.security;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.service.LoginAttemptService;
import com.skyline.org.common.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginSuccessHandlerTest {

    @Mock LoginAttemptService loginAttemptService;
    @Mock AuthAuditService authAuditService;
    @Mock ClientIpResolver clientIpResolver;

    LoginSuccessHandler handler;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.getAuth().setLoginSuccessUrl("/home");
        handler = new LoginSuccessHandler(loginAttemptService, authAuditService, properties, clientIpResolver);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");
    }

    @Test
    void recordsSuccessAndRedirectsToConfiguredUrl() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                "alice", "secret", java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(loginAttemptService).recordSuccess("alice", "203.0.113.10");
        verify(authAuditService).log(AuthEventType.LOGIN_SUCCESS, "alice", "203.0.113.10", null);
    }
}
