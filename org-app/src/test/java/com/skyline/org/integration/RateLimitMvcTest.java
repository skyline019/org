package com.skyline.org.integration;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RateLimitMvcTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void tightLoginLimit(DynamicPropertyRegistry registry) {
        registry.add("app.auth.rate-limit.login-per-minute", () -> "2");
    }

    @Test
    void htmlLoginRedirectsWhenRateLimited() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .with(remoteAddr("203.0.113.11"))
                            .header("Accept", "text/html")
                            .param("username", "nobody")
                            .param("password", "wrong"))
                    .andExpect(status().is3xxRedirection());
        }
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(remoteAddr("203.0.113.11"))
                        .header("Accept", "text/html")
                        .param("username", "nobody")
                        .param("password", "wrong"))
                .andExpect(redirectedUrl("/login?rateLimited"));
    }

    @Test
    void apiLoginReturns429WhenRateLimited() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .with(remoteAddr("203.0.113.12"))
                            .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                            .param("username", "nobody")
                            .param("password", "wrong"))
                    .andExpect(status().is3xxRedirection());
        }
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .with(remoteAddr("203.0.113.12"))
                        .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                        .param("username", "nobody")
                        .param("password", "wrong"))
                .andExpect(status().isTooManyRequests());
    }

    private static RequestPostProcessor remoteAddr(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
