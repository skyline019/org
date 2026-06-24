package com.skyline.org.auth.service;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.EmailVerificationToken;
import com.skyline.org.auth.event.VerificationEmailRequested;
import com.skyline.org.auth.repository.EmailVerificationTokenRepository;
import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.util.TokenGenerator;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final AuthAuditService authAuditService;
    private final Messages messages;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            AuthProperties authProperties,
            AuthAuditService authAuditService,
            Messages messages) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.authProperties = authProperties;
        this.authAuditService = authAuditService;
        this.messages = messages;
    }

    @Transactional
    public void sendVerificationEmail(User user) {
        issueAndSendToken(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userService.findByEmail(email);
        if (user == null || user.isEmailVerified()) {
            return;
        }
        issueAndSendToken(user);
        authAuditService.log(AuthEventType.RESEND_VERIFICATION, user.getUsername(), null, email);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = TokenGenerator.hashToken(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, messages.get("auth.error.token-invalid")));

        if (token.isUsed()) {
            throw new BusinessException(ErrorCode.TOKEN_USED, messages.get("auth.error.token-used"));
        }
        if (token.isExpired()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, messages.get("auth.error.token-expired"));
        }

        token.setUsed(true);
        tokenRepository.save(token);
        userService.verifyEmail(token.getUser());
        authAuditService.log(AuthEventType.EMAIL_VERIFIED, token.getUser().getUsername(), null, null);
    }

    private void issueAndSendToken(User user) {
        tokenRepository.invalidateActiveByUserId(user.getId());

        String rawToken = TokenGenerator.generateRawToken();
        String tokenHash = TokenGenerator.hashToken(rawToken);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plus(authProperties.getAuth().getToken().getEmailVerificationExpiry()));
        tokenRepository.save(token);

        String expiry = formatDuration(authProperties.getAuth().getToken().getEmailVerificationExpiry());
        String url = authProperties.getBaseUrl() + "/auth/verify-email/" + rawToken;
        eventPublisher.publishEvent(new VerificationEmailRequested(user.getEmail(), url, expiry));
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours >= 1) {
            return messages.get("auth.duration.hours", hours);
        }
        return messages.get("auth.duration.minutes", duration.toMinutes());
    }
}
