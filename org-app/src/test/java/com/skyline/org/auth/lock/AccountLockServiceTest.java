package com.skyline.org.auth.lock;

import com.skyline.org.auth.audit.AuthAuditPersistenceService;
import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.repository.AuthAuditEventRepository;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountLockServiceTest {

    private UserService userService;
    private AccountLockService accountLockService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        AuthProperties props = new AuthProperties();
        props.getAuth().getLock().setMaxAttempts(3);
        AuthAuditService audit = new AuthAuditService(props, mock(AuthAuditPersistenceService.class), new SimpleMeterRegistry());
        accountLockService = new AccountLockService(userService, props, audit);
        user = new User();
        user.setUsername("alice");
        user.setFailedAttempts(0);
    }

    @Test
    void locksAfterMaxAttempts() {
        accountLockService.recordFailedAttempt(user);
        accountLockService.recordFailedAttempt(user);
        accountLockService.recordFailedAttempt(user);
        assertThat(user.isLocked()).isTrue();
        verify(userService, org.mockito.Mockito.times(3)).save(user);
    }

    @Test
    void unlocksWhenExpired() {
        user.setLocked(true);
        user.setLockUntil(Instant.now().minusSeconds(60));
        accountLockService.unlockIfExpired(user);
        assertThat(user.isLocked()).isFalse();
    }
}
