package com.skyline.org.auth.lock;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Centralizes account lock / unlock rules used during authentication.
 */
@Service
public class AccountLockService {

    private final UserService userService;
    private final AuthProperties authProperties;
    private final AuthAuditService authAuditService;

    public AccountLockService(
            UserService userService,
            AuthProperties authProperties,
            AuthAuditService authAuditService) {
        this.userService = userService;
        this.authProperties = authProperties;
        this.authAuditService = authAuditService;
    }

    @Transactional
    public void unlockIfExpired(User user) {
        if (user.isLocked() && user.getLockUntil() != null && Instant.now().isAfter(user.getLockUntil())) {
            user.setLocked(false);
            user.setLockUntil(null);
            user.setFailedAttempts(0);
            userService.save(user);
        }
    }

    @Transactional
    public void recordFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= authProperties.getAuth().getLock().getMaxAttempts()) {
            user.setLocked(true);
            user.setLockUntil(Instant.now().plus(authProperties.getAuth().getLock().getDuration()));
            authAuditService.log(AuthEventType.ACCOUNT_LOCKED, user.getUsername(), null,
                    "attempts=" + attempts);
        }
        userService.save(user);
    }

    @Transactional
    public void resetLockState(User user) {
        user.setFailedAttempts(0);
        user.setLocked(false);
        user.setLockUntil(null);
        userService.save(user);
    }

    public boolean isCurrentlyLocked(User user) {
        return user.isLocked()
                && (user.getLockUntil() == null || user.getLockUntil().isAfter(Instant.now()));
    }

    public long minutesUntilUnlock(User user) {
        if (user.getLockUntil() == null) {
            return authProperties.getAuth().getLock().getDuration().toMinutes();
        }
        return Math.max(1, Duration.between(Instant.now(), user.getLockUntil()).toMinutes() + 1);
    }
}
