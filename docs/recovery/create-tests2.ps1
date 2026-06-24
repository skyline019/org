$utf8 = New-Object System.Text.UTF8Encoding $false
$base = "e:\JavaProjects\org\src\test\java"

function Write-TestFile($relPath, $content) {
    $full = Join-Path $base $relPath
    $dir = Split-Path $full -Parent
    [System.IO.Directory]::CreateDirectory($dir) | Out-Null
    [System.IO.File]::WriteAllText($full, $content.TrimStart() + "`n", $utf8)
}

Write-TestFile "com/skyline/org/auth/audit/AuthAuditServiceTest.java" @'
package com.skyline.org.auth.audit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthAuditServiceTest {

    @Test
    void incrementsCounterOnLog() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthAuditService service = new AuthAuditService(registry);
        service.log(AuthEventType.LOGIN_SUCCESS, "alice", "127.0.0.1", null);
        assertThat(registry.find("auth.audit.events").tag("event", "LOGIN_SUCCESS").counter().count()).isEqualTo(1.0);
    }
}
'@

Write-TestFile "com/skyline/org/auth/ratelimit/RateLimitServiceTest.java" @'
package com.skyline.org.auth.ratelimit;

import com.skyline.org.auth.config.AuthProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void allowsUnlimitedPaths() {
        AuthProperties props = new AuthProperties();
        RateLimitService service = new RateLimitService(props, (key, limit) -> true);
        assertThat(service.tryConsume("/home", "GET", "1.2.3.4")).isTrue();
    }

    @Test
    void delegatesLimitedPaths() {
        AuthProperties props = new AuthProperties();
        props.getAuth().getRateLimit().setLoginPerMinute(2);
        RateLimitService service = new RateLimitService(props, (key, limit) -> limit == 2);
        assertThat(service.tryConsume("/login", "POST", "1.2.3.4")).isTrue();
    }
}
'@

Write-TestFile "com/skyline/org/auth/ratelimit/RedisRateLimitBackendTest.java" @'
package com.skyline.org.auth.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitBackendTest {

    @Test
    void consumesWithinLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);
        when(redis.expire(anyString(), any())).thenReturn(true);

        RedisRateLimitBackend backend = new RedisRateLimitBackend(redis);
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
    }

    @Test
    void rejectsOverLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(4L);

        RedisRateLimitBackend backend = new RedisRateLimitBackend(redis);
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isFalse();
    }
}
'@

Write-TestFile "com/skyline/org/auth/lock/AccountLockServiceTest.java" @'
package com.skyline.org.auth.lock;

import com.skyline.org.auth.audit.AuthAuditService;
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
        AuthAuditService audit = new AuthAuditService(new SimpleMeterRegistry());
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
        verify(userService).save(user);
    }

    @Test
    void unlocksWhenExpired() {
        user.setLocked(true);
        user.setLockUntil(Instant.now().minusSeconds(60));
        accountLockService.unlockIfExpired(user);
        assertThat(user.isLocked()).isFalse();
    }
}
'@

Write-TestFile "com/skyline/org/auth/service/PasswordCheckServiceTest.java" @'
package com.skyline.org.auth.service;

import com.skyline.org.auth.dto.PasswordCheckResponse;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCheckServiceTest {

    @Test
    void returnsLocalizedRules() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.password.valid", Locale.getDefault(), "ok");
        source.addMessage("auth.password.rule.length.pass", Locale.getDefault(), "len");
        source.addMessage("auth.password.rule.upper.pass", Locale.getDefault(), "up");
        source.addMessage("auth.password.rule.lower.pass", Locale.getDefault(), "lo");
        source.addMessage("auth.password.rule.digit.pass", Locale.getDefault(), "di");
        source.addMessage("auth.password.rule.special.pass", Locale.getDefault(), "sp");
        Messages messages = new Messages(source);
        PasswordCheckService service = new PasswordCheckService(messages);

        PasswordCheckResponse response = service.check("Str0ng!Pass");
        assertThat(response.valid()).isTrue();
        assertThat(response.rules()).isNotEmpty();
    }
}
'@

Write-TestFile "com/skyline/org/auth/service/AvailabilityCheckServiceTest.java" @'
package com.skyline.org.auth.service;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.dto.AvailabilityResponse;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityCheckServiceTest {

    @Test
    void reportsUsernameAvailability() {
        UserService userService = mock(UserService.class);
        when(userService.isUsernameAvailable("newuser")).thenReturn(true);
        AuthProperties props = new AuthProperties();
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.check.username.available", Locale.getDefault(), "available");
        Messages messages = new Messages(source);
        AvailabilityCheckService service = new AvailabilityCheckService(userService, props, messages);

        AvailabilityResponse response = service.checkUsername("newuser");
        assertThat(response.available()).isTrue();
    }

    @Test
    void enumerationSafeModeHidesTakenStatus() {
        UserService userService = mock(UserService.class);
        AuthProperties props = new AuthProperties();
        props.getAuth().getCheck().setEnumerationSafe(true);
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.check.username.format-valid", Locale.getDefault(), "ok");
        Messages messages = new Messages(source);
        AvailabilityCheckService service = new AvailabilityCheckService(userService, props, messages);

        AvailabilityResponse response = service.checkUsername("anyone");
        assertThat(response.available()).isTrue();
    }
}
'@

Write-TestFile "com/skyline/org/auth/service/LoginAttemptServiceTest.java" @'
package com.skyline.org.auth.service;

import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.repository.LoginAttemptRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    @Test
    void resetsLockOnSuccess() {
        UserService userService = mock(UserService.class);
        LoginAttemptRepository repo = mock(LoginAttemptRepository.class);
        AccountLockService lockService = mock(AccountLockService.class);
        User user = new User();
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        LoginAttemptService service = new LoginAttemptService(userService, repo, lockService);

        service.recordSuccess("alice", "127.0.0.1");
        verify(lockService).resetLockState(user);
    }
}
'@

Write-TestFile "com/skyline/org/auth/schedule/AuthMaintenanceSchedulerTest.java" @'
package com.skyline.org.auth.schedule;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.repository.EmailVerificationTokenRepository;
import com.skyline.org.auth.repository.LoginAttemptRepository;
import com.skyline.org.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthMaintenanceSchedulerTest {

    @Test
    void purgesStaleRecords() {
        LoginAttemptRepository loginRepo = mock(LoginAttemptRepository.class);
        EmailVerificationTokenRepository emailRepo = mock(EmailVerificationTokenRepository.class);
        PasswordResetTokenRepository resetRepo = mock(PasswordResetTokenRepository.class);
        when(loginRepo.deleteByAttemptedAtBefore(any())).thenReturn(1);
        when(emailRepo.deleteByExpiresAtBefore(any())).thenReturn(2);
        when(resetRepo.deleteByExpiresAtBefore(any())).thenReturn(3);

        AuthProperties props = new AuthProperties();
        props.getAuth().getMaintenance().setLoginAttemptRetention(Duration.ofDays(30));
        props.getAuth().getMaintenance().setExpiredTokenRetention(Duration.ofDays(7));

        AuthMaintenanceScheduler scheduler = new AuthMaintenanceScheduler(loginRepo, emailRepo, resetRepo, props);
        scheduler.purgeStaleData();

        verify(loginRepo).deleteByAttemptedAtBefore(any());
        verify(emailRepo).deleteByExpiresAtBefore(any());
        verify(resetRepo).deleteByExpiresAtBefore(any());
    }
}
'@

Write-TestFile "com/skyline/org/auth/event/AuthMailEventListenerTest.java" @'
package com.skyline.org.auth.event;

import com.skyline.org.mail.MailService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthMailEventListenerTest {

    @Test
    void sendsVerificationEmail() {
        MailService mailService = mock(MailService.class);
        AuthMailEventListener listener = new AuthMailEventListener(mailService);
        listener.onVerificationEmail(new VerificationEmailRequested("a@b.com", "http://x", "24h"));
        verify(mailService).sendVerificationEmail("a@b.com", "http://x", "24h");
    }
}
'@

Write-Output "Created batch 2"
