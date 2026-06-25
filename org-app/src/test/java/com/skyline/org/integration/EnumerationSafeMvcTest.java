package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EnumerationSafeMvcTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired RegistrationService registrationService;

    private String username;

    @DynamicPropertySource
    static void enumerationSafe(DynamicPropertyRegistry registry) {
        registry.add("app.auth.check.enumeration-safe", () -> "true");
    }

    @BeforeEach
    void registerUser() {
        username = "enum" + System.nanoTime();
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword("Str0ng!Pass");
        request.setConfirmPassword("Str0ng!Pass");
        registrationService.register(request);
    }

    @Test
    void takenUsernameStillReportsAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check/username")
                        .param("value", username)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available", is(true)));
    }

    @Test
    void takenEmailStillReportsAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check/email")
                        .param("value", username + "@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available", is(true)));
    }
}
