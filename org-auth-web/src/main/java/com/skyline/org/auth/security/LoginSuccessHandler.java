package com.skyline.org.auth.security;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.MfaSessionKeys;
import com.skyline.org.auth.service.LoginAttemptService;
import com.skyline.org.common.web.ClientIpResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LoginAttemptService loginAttemptService;
    private final AuthAuditService authAuditService;
    private final AuthProperties authProperties;
    private final ClientIpResolver clientIpResolver;
    private final MfaService mfaService;

    public LoginSuccessHandler(
            LoginAttemptService loginAttemptService,
            AuthAuditService authAuditService,
            AuthProperties authProperties,
            ClientIpResolver clientIpResolver,
            MfaService mfaService) {
        this.loginAttemptService = loginAttemptService;
        this.authAuditService = authAuditService;
        this.authProperties = authProperties;
        this.clientIpResolver = clientIpResolver;
        this.mfaService = mfaService;
        setDefaultTargetUrl(authProperties.getAuth().getLoginSuccessUrl());
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        String ip = clientIpResolver.resolve(request);
        loginAttemptService.recordSuccess(username, ip);

        if (mfaService.requiresChallenge(username)) {
            request.getSession(true).setAttribute(MfaSessionKeys.MFA_VERIFIED, Boolean.FALSE);
            getRedirectStrategy().sendRedirect(request, response, "/auth/mfa/challenge");
            return;
        }

        request.getSession(true).setAttribute(MfaSessionKeys.MFA_VERIFIED, Boolean.TRUE);
        authAuditService.log(AuthEventType.LOGIN_SUCCESS, username, ip, null);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
