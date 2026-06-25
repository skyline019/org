package com.skyline.org.integration;

import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.repository.AuthAuditEventRepository;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@AutoConfigureMockMvc
class AuthAuditPersistenceIntegrationTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired AuthAuditEventRepository authAuditEventRepository;

    private String username;

    @DynamicPropertySource
    static void enableAuditPersistence(DynamicPropertyRegistry registry) {
        registry.add("app.auth.audit.persist", () -> "true");
    }

    @BeforeEach
    void registerAndVerify() throws Exception {
        username = unique("audit");
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
    void successfulLoginPersistsAuditEvent() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", "Str0ng!Pass"))
                .andExpect(redirectedUrl("/home"));

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(authAuditEventRepository.findByEventTypeAndSubjectOrderByOccurredAtDesc(
                        AuthEventType.LOGIN_SUCCESS, username)).isNotEmpty());
    }
}
