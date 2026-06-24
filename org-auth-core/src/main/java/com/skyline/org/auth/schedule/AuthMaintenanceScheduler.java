package com.skyline.org.auth.schedule;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.repository.EmailVerificationTokenRepository;
import com.skyline.org.auth.repository.LoginAttemptRepository;
import com.skyline.org.auth.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@EnableScheduling
public class AuthMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuthMaintenanceScheduler.class);

    private final LoginAttemptRepository loginAttemptRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthProperties authProperties;

    public AuthMaintenanceScheduler(
            LoginAttemptRepository loginAttemptRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AuthProperties authProperties) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.authProperties = authProperties;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeStaleData() {
        Instant loginCutoff = Instant.now().minus(authProperties.getAuth().getMaintenance().getLoginAttemptRetention());
        int loginRemoved = loginAttemptRepository.deleteByAttemptedAtBefore(loginCutoff);

        Instant tokenCutoff = Instant.now().minus(authProperties.getAuth().getMaintenance().getExpiredTokenRetention());
        int emailTokensRemoved = emailVerificationTokenRepository.deleteByExpiresAtBefore(tokenCutoff);
        int resetTokensRemoved = passwordResetTokenRepository.deleteByExpiresAtBefore(tokenCutoff);

        if (loginRemoved + emailTokensRemoved + resetTokensRemoved > 0) {
            log.info("Auth maintenance: removed {} login attempts, {} email tokens, {} reset tokens",
                    loginRemoved, emailTokensRemoved, resetTokensRemoved);
        }
    }
}
