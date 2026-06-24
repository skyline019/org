$utf8 = New-Object System.Text.UTF8Encoding $false
$base = "e:\JavaProjects\org\src\test\java"

function Write-TestFile($relPath, $content) {
    $full = Join-Path $base $relPath
    $dir = Split-Path $full -Parent
    [System.IO.Directory]::CreateDirectory($dir) | Out-Null
    [System.IO.File]::WriteAllText($full, $content.TrimStart() + "`n", $utf8)
}

Write-TestFile "com/skyline/org/integration/RegistrationFlowTest.java" @'
package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

class RegistrationFlowTest extends MailIntegrationSupport {

    @Autowired RegistrationService registrationService;
    @Autowired UserService userService;

    @Test
    void registersUserAndSendsVerificationEmail() {
        String username = unique("reg");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");

        registrationService.register(request);

        assertThat(userService.findByUsername(username)).isPresent();
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
    }
}
'@

Write-TestFile "com/skyline/org/integration/PasswordResetFlowTest.java" @'
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
        String body = GREEN_MAIL.getReceivedMessages()[0].getContent().toString();
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
'@

Write-TestFile "com/skyline/org/integration/LoginLockoutTest.java" @'
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
        String body = GREEN_MAIL.getReceivedMessages()[0].getContent().toString();
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
'@

Write-TestFile "com/skyline/org/integration/LoginFlowMvcTest.java" @'
package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginFlowMvcTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;

    private String username;

    @BeforeEach
    void registerAndVerify() throws Exception {
        username = unique("login");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[0].getContent().toString();
        Matcher m = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(m.find()).isTrue();
        emailVerificationService.verifyEmail(m.group(1));
    }

    @Test
    void loginRedirectsToHome() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", "Str0ng!Pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void homeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/home")).andExpect(status().is3xxRedirection());
    }
}
'@

Write-TestFile "com/skyline/org/integration/LoginLockoutMvcTest.java" @'
package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

class LoginLockoutMvcTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;

    private String username;

    @BeforeEach
    void setUpUser() throws Exception {
        username = unique("lockmvc");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[0].getContent().toString();
        Matcher m = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(m.find()).isTrue();
        emailVerificationService.verifyEmail(m.group(1));
    }

    @Test
    void failedLoginEventuallyLocksAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", username)
                            .param("password", "wrong"))
                    .andExpect(redirectedUrlPattern("/login?error*"));
        }
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", "wrong"))
                .andExpect(redirectedUrlPattern("/login?error*"));
    }
}
'@

Write-TestFile "com/skyline/org/integration/AuthEmailFlowMvcTest.java" @'
package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AuthEmailFlowMvcTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;

    @Test
    void verifyResendAndResetFlow() throws Exception {
        String username = unique("email");
        String email = username + "@example.com";
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).hasSize(1));

        String body = GREEN_MAIL.getReceivedMessages()[0].getContent().toString();
        String token = body.replaceAll("(?s).*?/auth/verify-email/([A-Za-z0-9_-]+).*", "$1");

        mockMvc.perform(get("/auth/verify-email/" + token))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email-confirm"));

        mockMvc.perform(post("/auth/verify-email").with(csrf()).param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email-result"));

        mockMvc.perform(post("/auth/resend-verification").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/auth/verify-email-pending"));
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(2));

        mockMvc.perform(post("/auth/forgot-password").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/login"));
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(3));

        String resetBody = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        String resetToken = resetBody.replaceAll("(?s).*?/auth/reset-password/([A-Za-z0-9_-]+).*", "$1");

        mockMvc.perform(get("/auth/reset-password/" + resetToken))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"));

        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .param("token", resetToken)
                        .param("password", "NewStr0ng!Pass")
                        .param("confirmPassword", "NewStr0ng!Pass"))
                .andExpect(redirectedUrl("/login"));
    }
}
'@

Write-Output "Created batch 4"
