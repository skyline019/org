package com.skyline.org.auth.config;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired SecurityFilterChain securityFilterChain;

    @Test
    void securityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void publicAuthPagesAreAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void checkApiIsAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/check/username").param("value", "demo_user"))
                .andExpect(status().isOk());
    }

    @Test
    void homeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void staticResourcesArePublic() throws Exception {
        mockMvc.perform(get("/css/auth.css")).andExpect(status().isOk());
    }

    @Test
    void loginPageIncludesBaselineSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
}
