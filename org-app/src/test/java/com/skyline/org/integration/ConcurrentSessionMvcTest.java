package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.security.CustomUserDetailsService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
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
class ConcurrentSessionMvcTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired SessionRegistry sessionRegistry;
    @Autowired CustomUserDetailsService userDetailsService;

    private String username;
    private String password;

    @BeforeEach
    void registerVerifiedUser() throws Exception {
        username = unique("sess");
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
    }

    @Test
    void fourthLoginExpiresOldestSession() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .session(new MockHttpSession())
                            .param("username", username)
                            .param("password", password))
                    .andExpect(redirectedUrl("/home"));
        }

        UserDetails principal = userDetailsService.loadUserByUsername(username);
        await().atMost(5, SECONDS).untilAsserted(() -> {
            assertThat(sessionRegistry.getAllSessions(principal, false)).hasSize(3);
            assertThat(sessionRegistry.getAllSessions(principal, true)).hasSize(4);
        });

        long expiredSessions = sessionRegistry.getAllSessions(principal, true).stream()
                .filter(SessionInformation::isExpired)
                .count();
        assertThat(expiredSessions).isEqualTo(1);
    }
}
