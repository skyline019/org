package com.skyline.org.integration;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "oauth2"})
@AutoConfigureMockMvc
class OAuthRateLimitMvcTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;

    @DynamicPropertySource
    static void tightOAuthLimit(DynamicPropertyRegistry registry) {
        registry.add("app.auth.rate-limit.login-per-minute", () -> "2");
    }

    @Test
    void oauthAuthorizationRedirectIsRateLimited() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get("/oauth2/authorization/github")
                            .with(remoteAddr("203.0.113.21"))
                            .header("Accept", "text/html"))
                    .andExpect(status().is3xxRedirection());
        }
        mockMvc.perform(get("/oauth2/authorization/github")
                        .with(remoteAddr("203.0.113.21"))
                        .header("Accept", "text/html"))
                .andExpect(redirectedUrl("/login?rateLimited"));
    }

    private static RequestPostProcessor remoteAddr(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
