package com.skyline.org.integration;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.ratelimit.RateLimitBackend;
import com.skyline.org.auth.ratelimit.RedisRateLimitBackend;
import com.skyline.org.testsupport.RedisIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnabledIf("com.skyline.org.testsupport.RedisAvailableCondition#isAvailable")
@ActiveProfiles({"test", "prod", "redis"})
@AutoConfigureMockMvc
class ProdProfileSmokeTest extends RedisIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired Environment environment;
    @Autowired AuthProperties authProperties;
    @Autowired RateLimitBackend rateLimitBackend;

    @DynamicPropertySource
    static void prodOverrides(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", () ->
                "jdbc:mysql://localhost:3306/org_auth_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
        registry.add("DB_USERNAME", () -> System.getenv().getOrDefault("DB_USERNAME", "Voyager"));
        registry.add("DB_PASSWORD", () -> System.getenv().getOrDefault("DB_PASSWORD", ""));
        registry.add("APP_BASE_URL", () -> "https://auth.example.com");
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "smtp-user");
        registry.add("MAIL_PASSWORD", () -> "smtp-pass");
        registry.add("MAIL_FROM", () -> "noreply@example.com");
    }

    @Test
    void prodSecurityDefaultsAreActive() {
        assertThat(authProperties.getAuth().getCheck().isEnumerationSafe()).isTrue();
        assertThat(environment.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
        assertThat(environment.getProperty("app.trusted-proxy.enabled")).isEqualTo("true");
    }

    @Test
    void usesRedisRateLimitBackend() {
        assertThat(rateLimitBackend).isInstanceOf(RedisRateLimitBackend.class);
    }

    @Test
    void swaggerUiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void loginPageIncludesHstsAndBaselineHeaders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("Content-Security-Policy"));
    }
}
