package com.skyline.org.integration;

import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.auth.repository.UserMfaRecoveryCodeRepository;
import com.skyline.org.testsupport.MailIntegrationSupport;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.repository.UserRepository;
import com.skyline.org.user.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminMfaEnforcementMvcTest extends MailIntegrationSupport {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RoleService roleService;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired TotpMfaService totpMfaService;
    @Autowired MfaService mfaService;
    @Autowired UserMfaRecoveryCodeRepository recoveryCodeRepository;

    private static final String TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private String username;

    @DynamicPropertySource
    static void enableAdminMfa(DynamicPropertyRegistry registry) {
        registry.add("app.auth.mfa.enabled", () -> "true");
        registry.add("app.auth.mfa.enforce-for-roles", () -> "ROLE_ADMIN");
        registry.add("spring.session.store-type", () -> "none");
        registry.add("app.auth.mfa.secret-encryption.mode", () -> "local");
        registry.add("app.auth.mfa.secret-encryption.local-key", () -> TEST_ENCRYPTION_KEY);
    }

    @BeforeEach
    void createAdminUser() {
        username = unique("adminmfa");
        Role adminRole = roleService.requireRole(RoleService.ADMIN_ROLE);
        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(username + "@example.com");
        admin.setPasswordHash(passwordEncoder.encode("Str0ng!Pass"));
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.addRole(adminRole);
        userRepository.saveAndFlush(admin);
    }

    @Test
    void adminWithoutMfaIsRedirectedToSetup() throws Exception {
        mockMvc.perform(get("/home").with(user(username).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/setup"));
    }

    @Test
    void adminCanEnrollAndAccessHome() throws Exception {
        MvcResult setupPage = mockMvc.perform(get("/auth/mfa/setup").with(user(username).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        Matcher secretMatcher = Pattern.compile("<code>([A-Z2-7=]+)</code>").matcher(setupPage.getResponse().getContentAsString());
        assertThat(secretMatcher.find()).isTrue();
        String plainSecret = secretMatcher.group(1);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/auth/mfa/enable")
                        .with(user(username).roles("ADMIN"))
                        .session(session)
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/recovery-codes"));

        assertThat(recoveryCodeRepository.count()).isGreaterThan(0);
    }

    @Test
    void adminCannotDisableMandatoryMfa() throws Exception {
        MvcResult setupPage = mockMvc.perform(get("/auth/mfa/setup").with(user(username).roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();
        Matcher secretMatcher = Pattern.compile("<code>([A-Z2-7=]+)</code>").matcher(setupPage.getResponse().getContentAsString());
        assertThat(secretMatcher.find()).isTrue();
        String plainSecret = secretMatcher.group(1);

        mockMvc.perform(post("/auth/mfa/enable")
                        .with(user(username).roles("ADMIN"))
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/recovery-codes"));

        mockMvc.perform(post("/auth/mfa/disable")
                        .with(user(username).roles("ADMIN"))
                        .with(csrf())
                        .param("code", totpMfaService.currentCodeForSecret(plainSecret)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/mfa/setup"));

        assertThat(mfaService.isEnrolled(username)).isTrue();
    }
}
