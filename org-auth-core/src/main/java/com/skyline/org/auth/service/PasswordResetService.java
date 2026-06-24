package com.skyline.org.auth.service;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.dto.ResetPasswordRequest;
import com.skyline.org.auth.entity.PasswordResetToken;
import com.skyline.org.auth.event.PasswordResetEmailRequested;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.repository.PasswordResetTokenRepository;
import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.util.TokenGenerator;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetService {

    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountLockService accountLockService;
    private final AuthProperties authProperties;
    private final AuthAuditService authAuditService;
    private final Messages messages;

    public PasswordResetService(
            UserService userService,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            AccountLockService accountLockService,
            AuthProperties authProperties,
            AuthAuditService authAuditService,
            Messages messages) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.accountLockService = accountLockService;
        this.authProperties = authProperties;
        this.authAuditService = authAuditService;
        this.messages = messages;
    }

    @Transactional
    public void requestReset(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return;
        }

        tokenRepository.invalidateActiveByUserId(user.getId());

        String rawToken = TokenGenerator.generateRawToken();
        String tokenHash = TokenGenerator.hashToken(rawToken);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plus(authProperties.getAuth().getToken().getPasswordResetExpiry()));
        tokenRepository.save(token);

        String expiry = formatDuration(authProperties.getAuth().getToken().getPasswordResetExpiry());
        String url = authProperties.getBaseUrl() + "/auth/reset-password/" + rawToken;
        eventPublisher.publishEvent(new PasswordResetEmailRequested(user.getEmail(), url, expiry));
        authAuditService.log(AuthEventType.PASSWORD_RESET_REQUEST, user.getUsername(), null, email);
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String tokenHash = TokenGenerator.hashToken(rawToken);
        return tokenRepository.findByTokenHash(tokenHash)
                .map(token -> !token.isUsed() && !token.isExpired())
                .orElse(false);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, messages.get("auth.register.mismatch"));
        }

        String tokenHash = TokenGenerator.hashToken(request.getToken());
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, messages.get("auth.error.token-reset-invalid")));

        if (token.isUsed()) {
            throw new BusinessException(ErrorCode.TOKEN_USED, messages.get("auth.error.token-reset-used"));
        }
        if (token.isExpired()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, messages.get("auth.error.token-reset-expired"));
        }

        token.setUsed(true);
        tokenRepository.save(token);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        accountLockService.resetLockState(user);
        authAuditService.log(AuthEventType.PASSWORD_RESET, user.getUsername(), null, null);
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours >= 1) {
            return messages.get("auth.duration.hours", hours);
        }
        return messages.get("auth.duration.minutes", duration.toMinutes());
    }
}
