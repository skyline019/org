package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.entity.UserTotpCredential;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.auth.repository.UserTotpRepository;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MfaMvcIntegrationTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired TotpMfaService totpMfaService;
    @Autowired UserTotpRepository userTotpRepository;
    @Autowired UserService userService;

    private String username;

    @DynamicPropertySource
    static void enableMfa(DynamicPropertyRegistry registry) {
        registry.add("app.auth.mfa.enabled", () -> "true");
    }

    @BeforeEach
    void registerVerifiedUser() throws Exception {
        username = unique("mfa");
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        Matcher matcher = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        emailVerificationService.verifyEmail(matcher.group(1));
    }

    @Test
    void mfaFilterBlocksHomeUntilVerified() throws Exception {
        String secret = totpMfaService.generateSecret();
        User user = userService.findByUsername(username).orElseThrow();
        UserTotpCredential credential = new UserTotpCredential(user, secret);
        credential.setEnabled(true);
        userTotpRepository.saveAndFlush(credential);

        mockMvc.perform(get("/home").with(user(username)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/challenge"));
    }
}
