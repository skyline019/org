package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.LoginAttemptService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

class LoginLockoutTest extends MailIntegrationSupport {

    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired LoginAttemptService loginAttemptService;
    @Autowired UserService userService;
    @Autowired AccountLockService accountLockService;

    private User user;

    @BeforeEach
    void setUpVerifiedUser() throws Exception {
        String username = unique("lock");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        Matcher m = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(m.find()).isTrue();
        emailVerificationService.verifyEmail(m.group(1));
        user = userService.findByUsername(username).orElseThrow();
    }

    @Test
    void locksAccountAfterRepeatedFailures() {
        for (int i = 0; i < 5; i++) {
            loginAttemptService.recordFailure(user.getUsername(), "127.0.0.1");
        }
        User reloaded = userService.findByUsername(user.getUsername()).orElseThrow();
        assertThat(accountLockService.isCurrentlyLocked(reloaded)).isTrue();
    }
}
