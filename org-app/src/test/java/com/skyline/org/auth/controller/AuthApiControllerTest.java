package com.skyline.org.auth.controller;

import com.skyline.org.auth.dto.AvailabilityResponse;
import com.skyline.org.auth.dto.PasswordCheckResponse;
import com.skyline.org.auth.dto.PasswordRuleItem;
import com.skyline.org.auth.service.AvailabilityCheckService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordCheckService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

    @Mock AvailabilityCheckService availabilityCheckService;
    @Mock RegistrationService registrationService;
    @Mock EmailVerificationService emailVerificationService;
    @Mock PasswordCheckService passwordCheckService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.resend.success", Locale.getDefault(), "sent");
        source.addMessage("auth.validation.username.blank", Locale.getDefault(), "blank");
        source.addMessage("auth.validation.username.length", Locale.getDefault(), "length");
        source.addMessage("auth.validation.username.format", Locale.getDefault(), "format");
        source.addMessage("auth.validation.email.blank", Locale.getDefault(), "blank");
        source.addMessage("auth.validation.email.format", Locale.getDefault(), "format");
        source.addMessage("auth.validation.password.blank", Locale.getDefault(), "blank");
        source.addMessage("auth.validation.confirm.blank", Locale.getDefault(), "blank");
        source.addMessage("auth.password.valid", Locale.getDefault(), "ok");
        source.addMessage("auth.password.invalid", Locale.getDefault(), "invalid");
        source.addMessage("auth.password.empty", Locale.getDefault(), "empty");
        source.addMessage("auth.password.rule.length.pass", Locale.getDefault(), "len");
        source.addMessage("auth.password.rule.upper.pass", Locale.getDefault(), "up");
        source.addMessage("auth.password.rule.lower.pass", Locale.getDefault(), "lo");
        source.addMessage("auth.password.rule.digit.pass", Locale.getDefault(), "di");
        source.addMessage("auth.password.rule.special.pass", Locale.getDefault(), "sp");
        Messages messages = new Messages(source);
        AuthApiController controller = new AuthApiController(
                availabilityCheckService, registrationService, emailVerificationService, passwordCheckService, messages);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(source);
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
    }

    @Test
    void checkUsername() throws Exception {
        when(availabilityCheckService.checkUsername("alice")).thenReturn(new AvailabilityResponse(true, "ok"));
        mockMvc.perform(get("/api/v1/auth/check/username").param("value", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void registerViaJson() throws Exception {
        doNothing().when(registrationService).register(any());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user1","email":"user1@ex.com","password":"Str0ng!Pass","confirmPassword":"Str0ng!Pass"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void checkPassword() throws Exception {
        when(passwordCheckService.check("x")).thenReturn(new PasswordCheckResponse(false, 0, List.of(new PasswordRuleItem("bad", false)), "bad"));
        mockMvc.perform(get("/api/v1/auth/check/password").param("value", "x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false));
    }
}
