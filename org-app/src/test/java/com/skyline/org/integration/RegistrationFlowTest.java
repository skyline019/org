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
