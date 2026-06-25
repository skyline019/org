package com.skyline.org.auth.controller;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordResetService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AuthPageControllerTest {

    @Mock RegistrationService registrationService;
    @Mock EmailVerificationService emailVerificationService;
    @Mock PasswordResetService passwordResetService;

    MockMvc mockMvc;
    AuthProperties authProperties = new AuthProperties();

    @BeforeEach
    void setUp() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.login.error", Locale.getDefault(), "error");
        Messages messages = new Messages(source);
        AuthPageController controller = new AuthPageController(
                registrationService, emailVerificationService, passwordResetService, messages,
                authProperties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void showsLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void showsRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }
}
