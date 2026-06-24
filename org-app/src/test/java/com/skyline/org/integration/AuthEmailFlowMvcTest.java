package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

@AutoConfigureMockMvc
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

        mockMvc.perform(post("/auth/resend-verification").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/auth/verify-email-pending"));
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).hasSizeGreaterThanOrEqualTo(2));

        String latestBody = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        String verifyToken = latestBody.replaceAll("(?s).*?/auth/verify-email/([A-Za-z0-9_-]+).*", "$1");

        mockMvc.perform(post("/auth/verify-email").with(csrf()).param("token", verifyToken))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/verify-email-result"));

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
