package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.dto.ResetPasswordRequest;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordResetService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.util.TokenGenerator;
import com.skyline.org.testsupport.MailIntegrationSupport;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

class PasswordResetFlowTest extends MailIntegrationSupport {

    private static final Pattern RESET_LINK = Pattern.compile("/auth/reset-password/([A-Za-z0-9_-]+)");

    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired PasswordResetService passwordResetService;
    @Autowired UserService userService;
    @Autowired PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void registerVerifiedUser() throws Exception {
        String username = unique("reset");
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
        GREEN_MAIL.purgeEmailFromAllMailboxes();
    }

    @Test
    void resetsPasswordViaEmailLink() throws Exception {
        passwordResetService.requestReset(user.getEmail());
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        MimeMessage message = GREEN_MAIL.getReceivedMessages()[0];
        Matcher m = RESET_LINK.matcher(message.getContent().toString());
        assertThat(m.find()).isTrue();
        String rawToken = m.group(1);
        assertThat(passwordResetService.isTokenValid(rawToken)).isTrue();

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken(rawToken);
        reset.setPassword("NewStr0ng!Pass");
        reset.setConfirmPassword("NewStr0ng!Pass");
        passwordResetService.resetPassword(reset);

        User updated = userService.findByUsername(user.getUsername()).orElseThrow();
        assertThat(passwordEncoder.matches("NewStr0ng!Pass", updated.getPasswordHash())).isTrue();
    }
}
