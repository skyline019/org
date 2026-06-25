package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.mfa.MfaEnrollmentResult;
import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.auth.mfa.crypto.TotpSecretCryptoService;
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
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@AutoConfigureMockMvc
class MfaControllerMvcTest extends MailIntegrationSupport {

    private static final String TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired TotpMfaService totpMfaService;
    @Autowired MfaService mfaService;
    @Autowired TotpSecretCryptoService totpSecretCryptoService;

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
        username = unique("mfactl");
        password = "Str0ng!Pass";
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword(password);
        request.setConfirmPassword(password);
        registrationService.register(request);
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        Matcher matcher = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        emailVerificationService.verifyEmail(matcher.group(1));
    }

    @Test
    void challengePageRedirectsWhenNotEnrolled() throws Exception {
        mockMvc.perform(get("/auth/mfa/challenge").with(user(username)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void setupPageStartsEnrollmentAndShowsSecret() throws Exception {
        mockMvc.perform(get("/auth/mfa/setup").with(user(username)))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/mfa-setup"));

        assertThat(totpSecretCryptoService.isSealed(
                mfaService.findCredential(username).orElseThrow().getSecret())).isTrue();
    }

    @Test
    void enableWithInvalidCodeReturnsToSetup() throws Exception {
        mfaService.beginEnrollment(username);

        mockMvc.perform(post("/auth/mfa/enable")
                        .with(user(username))
                        .with(csrf())
                        .param("code", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/setup"));
    }

    @Test
    void enableRedirectsToRecoveryCodesPage() throws Exception {
        String plainSecret = mfaService.beginEnrollment(username);

        mockMvc.perform(post("/auth/mfa/enable")
                        .with(user(username))
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/recovery-codes"));
    }

    @Test
    void recoveryCodesPageRequiresFlashAttribute() throws Exception {
        mockMvc.perform(get("/auth/mfa/recovery-codes").with(user(username)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/setup"));
    }

    @Test
    void challengeAcceptsRecoveryCode() throws Exception {
        String plainSecret = mfaService.beginEnrollment(username);
        MfaEnrollmentResult enrollment = mfaService.confirmEnrollment(
                username,
                totpMfaService.currentCodeForSecret(plainSecret));
        String recoveryCode = enrollment.recoveryCodes().get(0);

        mockMvc.perform(post("/auth/mfa/challenge")
                        .with(user(username))
                        .with(csrf())
                        .param("code", recoveryCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void enrolledUserSeesAlreadyEnrolledOnSetup() throws Exception {
        enrollUser(username);

        mockMvc.perform(get("/auth/mfa/setup").with(user(username)))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/mfa-setup"));
    }

    @Test
    void disableTurnsOffMfaForRegularUser() throws Exception {
        String plainSecret = enrollUser(username);

        mockMvc.perform(post("/auth/mfa/disable")
                        .with(user(username))
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/setup"));

        assertThat(mfaService.isEnrolled(username)).isFalse();
    }

    @Test
    void recoveryCodesPageRendersAfterEnableRedirect() throws Exception {
        String plainSecret = mfaService.beginEnrollment(username);

        mockMvc.perform(post("/auth/mfa/enable")
                        .with(user(username))
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/recovery-codes"));
    }

    private String enrollUser(String user) {
        String plainSecret = mfaService.beginEnrollment(user);
        mfaService.confirmEnrollment(user, totpMfaService.currentCodeForSecret(plainSecret));
        return plainSecret;
    }
}
