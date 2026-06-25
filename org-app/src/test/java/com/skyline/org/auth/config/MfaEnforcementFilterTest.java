package com.skyline.org.auth.config;

import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.MfaSessionKeys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaEnforcementFilterTest {

    @Mock MfaService mfaService;
    @Mock FilterChain filterChain;

    MfaEnforcementFilter filter;

    @BeforeEach
    void setUp() {
        filter = new MfaEnforcementFilter(mfaService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughWhenFeatureDisabled() throws Exception {
        when(mfaService.isFeatureEnabled()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void redirectsToSetupWhenMandatoryEnrollmentRequired() throws Exception {
        when(mfaService.isFeatureEnabled()).thenReturn(true);
        authenticate("admin");
        when(mfaService.requiresMandatoryEnrollment(eq("admin"), any())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/mfa/setup");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void redirectsToChallengeWhenEnrolledAndNotVerified() throws Exception {
        when(mfaService.isFeatureEnabled()).thenReturn(true);
        authenticate("alice");
        when(mfaService.requiresMandatoryEnrollment(eq("alice"), any())).thenReturn(false);
        when(mfaService.requiresChallenge("alice")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/mfa/challenge");
    }

    @Test
    void allowsRequestWhenMfaVerifiedInSession() throws Exception {
        when(mfaService.isFeatureEnabled()).thenReturn(true);
        authenticate("alice");
        when(mfaService.requiresMandatoryEnrollment(eq("alice"), any())).thenReturn(false);
        when(mfaService.requiresChallenge("alice")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MfaSessionKeys.MFA_VERIFIED, Boolean.TRUE);
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void exemptsMfaPaths() throws Exception {
        when(mfaService.isFeatureEnabled()).thenReturn(true);
        authenticate("alice");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/mfa/challenge");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(mfaService, never()).requiresChallenge("alice");
    }

    private static void authenticate(String username) {
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
