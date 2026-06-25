package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MfaMvcIntegrationTest extends MailIntegrationSupport {

    private static final String TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired TotpMfaService totpMfaService;
    @Autowired MfaService mfaService;

    private String username;
    private String password;

    @DynamicPropertySource
    static void enableMfa(DynamicPropertyRegistry registry) {
        registry.add("app.auth.mfa.enabled", () -> "true");
        registry.add("spring.session.store-type", () -> "none");
        registry.add("app.auth.mfa.secret-encryption.mode", () -> "local");
        registry.add("app.auth.mfa.secret-encryption.local-key", () -> TEST_ENCRYPTION_KEY);
    }

    @BeforeEach
    void registerVerifiedUser() throws Exception {
        username = unique("mfa");
        password = "Str0ng!Pass";
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword(password);
        request.setConfirmPassword(password);
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        Matcher matcher = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        emailVerificationService.verifyEmail(matcher.group(1));

        String plainSecret = mfaService.beginEnrollment(username);
        mfaService.confirmEnrollment(username, totpMfaService.currentCodeForSecret(plainSecret));
    }

    @Test
    void mfaFilterBlocksHomeUntilVerified() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("MFA_VERIFIED", Boolean.FALSE);

        mockMvc.perform(get("/home").with(user(username)).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/challenge"));
    }

    @Test
    void challengeWithValidTotpGrantsAccessToHome() throws Exception {
        String plainSecret = mfaService.resolvePlainSecret(username);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/auth/mfa/challenge")
                        .with(user(username))
                        .session(session)
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void enrollFlowShowsRecoveryCodes() throws Exception {
        String freshUser = unique("enroll");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(freshUser);
        request.setEmail(freshUser + "@example.com");
        request.setPassword(password);
        request.setConfirmPassword(password);
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        Matcher matcher = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        emailVerificationService.verifyEmail(matcher.group(1));

        mockMvc.perform(get("/auth/mfa/setup").with(user(freshUser)))
                .andExpect(status().isOk());

        String storedSecret = mfaService.resolvePlainSecret(freshUser);

        mockMvc.perform(post("/auth/mfa/enable")
                        .with(user(freshUser))
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(storedSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/recovery-codes"));
    }
}
