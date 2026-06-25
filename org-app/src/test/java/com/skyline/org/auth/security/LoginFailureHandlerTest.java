package com.skyline.org.auth.security;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.service.LoginAttemptService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.web.ClientIpResolver;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

    @Mock LoginAttemptService loginAttemptService;
    @Mock UserService userService;
    @Mock AccountLockService accountLockService;
    @Mock AuthAuditService authAuditService;
    @Mock ClientIpResolver clientIpResolver;

    LoginFailureHandler handler;
    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new LoginFailureHandler(
                loginAttemptService, userService, accountLockService, authAuditService, messages(), clientIpResolver);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setParameter("username", "alice");
        org.mockito.Mockito.when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("127.0.0.1");
        FlashMapManager flashMapManager = new SessionFlashMapManager();
        request.setAttribute(FlashMapManager.class.getName(), flashMapManager);
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
    }

    @Test
    void recordsFailureForBadCredentials() throws Exception {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(loginAttemptService).recordFailure("alice", "127.0.0.1");
        verify(authAuditService).log(
                eq(AuthEventType.LOGIN_FAILURE), eq("alice"), eq("127.0.0.1"), eq("BadCredentialsException"));
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error");
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        assertNotNull(flashMap);
        assertEquals("Invalid username or password", flashMap.get("errorMessage"));
    }

    @Test
    void doesNotRecordFailureForDisabledAccount() throws Exception {
        handler.onAuthenticationFailure(request, response, new DisabledException("disabled"));

        verify(loginAttemptService, org.mockito.Mockito.never()).recordFailure(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        assertEquals("Please verify your email before logging in", flashMap.get("errorMessage"));
    }

    @Test
    void showsRemainingLockMinutes() throws Exception {
        User user = new User();
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(accountLockService.isCurrentlyLocked(user)).thenReturn(true);
        when(accountLockService.minutesUntilUnlock(user)).thenReturn(12L);

        handler.onAuthenticationFailure(request, response, new LockedException("locked"));

        verify(loginAttemptService, org.mockito.Mockito.never()).recordFailure(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        assertEquals("Account is locked, please try again in 12 minutes", flashMap.get("errorMessage"));
    }

    private static Messages messages() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.login.error", java.util.Locale.getDefault(), "Invalid username or password");
        source.addMessage("auth.login.disabled", java.util.Locale.getDefault(), "Please verify your email before logging in");
        source.addMessage("auth.login.locked-minutes", java.util.Locale.getDefault(), "Account is locked, please try again in {0} minutes");
        source.addMessage("auth.login.locked", java.util.Locale.getDefault(), "Account is locked, please try again later");
        return new Messages(source);
    }
}
