package com.skyline.org.auth.security;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.service.LoginAttemptService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.web.ClientIpResolver;
import com.skyline.org.user.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;

@Component
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final UserService userService;
    private final AccountLockService accountLockService;
    private final AuthAuditService authAuditService;
    private final Messages messages;

    public LoginFailureHandler(
            LoginAttemptService loginAttemptService,
            UserService userService,
            AccountLockService accountLockService,
            AuthAuditService authAuditService,
            Messages messages) {
        this.loginAttemptService = loginAttemptService;
        this.userService = userService;
        this.accountLockService = accountLockService;
        this.authAuditService = authAuditService;
        this.messages = messages;
        setDefaultFailureUrl("/login?error");
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        AuthenticationException resolved = unwrap(exception);
        String username = request.getParameter("username");
        String ip = ClientIpResolver.resolve(request);

        if (shouldRecordFailedAttempt(resolved) && username != null && !username.isBlank()) {
            loginAttemptService.recordFailure(username, ip);
        }

        authAuditService.log(AuthEventType.LOGIN_FAILURE, username, ip, resolved.getClass().getSimpleName());

        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        if (flashMap != null) {
            flashMap.put("errorMessage", resolveMessage(resolved, username));
        }
        getRedirectStrategy().sendRedirect(request, response, "/login?error");
    }

    private static boolean shouldRecordFailedAttempt(AuthenticationException exception) {
        return !(exception instanceof DisabledException || exception instanceof LockedException);
    }

    private static AuthenticationException unwrap(AuthenticationException exception) {
        if (exception instanceof InternalAuthenticationServiceException internal
                && internal.getCause() instanceof AuthenticationException cause) {
            return cause;
        }
        return exception;
    }

    private String resolveMessage(AuthenticationException exception, String username) {
        if (exception instanceof DisabledException) {
            return msg("auth.login.disabled");
        }
        if (exception instanceof LockedException && username != null && !username.isBlank()) {
            return userService.findByUsername(username)
                    .filter(accountLockService::isCurrentlyLocked)
                    .map(user -> messages.get("auth.login.locked-minutes",
                            accountLockService.minutesUntilUnlock(user)))
                    .orElse(msg("auth.login.locked"));
        }
        return msg("auth.login.error");
    }

    private String msg(String key) {
        return messages.get(key);
    }
}
