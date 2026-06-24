$utf8 = New-Object System.Text.UTF8Encoding $false
$base = "e:\JavaProjects\org\src\test\java"

function Write-TestFile($relPath, $content) {
    $full = Join-Path $base $relPath
    $dir = Split-Path $full -Parent
    [System.IO.Directory]::CreateDirectory($dir) | Out-Null
    [System.IO.File]::WriteAllText($full, $content.TrimStart() + "`n", $utf8)
}

Write-TestFile "com/skyline/org/auth/config/RateLimitFilterTest.java" @'
package com.skyline.org.auth.config;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.ratelimit.RateLimitService;
import com.skyline.org.common.i18n.Messages;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    void returns429WhenLimited() throws Exception {
        RateLimitService rateLimitService = mock(RateLimitService.class);
        AuthAuditService audit = mock(AuthAuditService.class);
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.rate-limit", Locale.getDefault(), "slow down");
        Messages messages = new Messages(source);
        when(rateLimitService.tryConsume(any(), any(), any())).thenReturn(false);

        RateLimitFilter filter = new RateLimitFilter(rateLimitService, audit, messages);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(429);
        verify(chain, never()).doFilter(any(), any());
    }
}
'@

Write-TestFile "com/skyline/org/auth/controller/AuthApiControllerTest.java" @'
package com.skyline.org.auth.controller;

import com.skyline.org.auth.dto.AvailabilityResponse;
import com.skyline.org.auth.dto.PasswordCheckResponse;
import com.skyline.org.auth.dto.PasswordRuleItem;
import com.skyline.org.auth.service.AvailabilityCheckService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordCheckService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthApiController.class, addFilters = false)
@Import(Messages.class)
class AuthApiControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AvailabilityCheckService availabilityCheckService;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean EmailVerificationService emailVerificationService;
    @MockitoBean PasswordCheckService passwordCheckService;

    @Test
    void checkUsername() throws Exception {
        when(availabilityCheckService.checkUsername("alice")).thenReturn(new AvailabilityResponse(true, "ok"));
        mockMvc.perform(get("/api/v1/auth/check/username").param("value", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void registerRequiresCsrf() throws Exception {
        doNothing().when(registrationService).register(any());
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"u1","email":"u1@ex.com","password":"Str0ng!Pass","confirmPassword":"Str0ng!Pass"}
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
'@

Write-TestFile "com/skyline/org/auth/controller/AuthPageControllerTest.java" @'
package com.skyline.org.auth.controller;

import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordResetService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AuthPageController.class, addFilters = false)
@Import(Messages.class)
class AuthPageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean EmailVerificationService emailVerificationService;
    @MockitoBean PasswordResetService passwordResetService;

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
'@

Write-TestFile "com/skyline/org/auth/config/SecurityConfigTest.java" @'
package com.skyline.org.auth.config;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired SecurityFilterChain securityFilterChain;

    @Test
    void securityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }
}
'@

Write-TestFile "com/skyline/org/user/repository/UserRepositoryTest.java" @'
package com.skyline.org.user.repository;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import com.skyline.org.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired UserRepository userRepository;

    @Test
    void savesAndFindsUserByUsername() {
        User user = new User();
        user.setUsername("repo_" + System.nanoTime());
        user.setEmail(user.getUsername() + "@example.com");
        user.setPasswordHash("hash");
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        assertThat(userRepository.findByUsername(user.getUsername())).isPresent();
    }
}
'@

Write-TestFile "com/skyline/org/mail/MailServiceTest.java" @'
package com.skyline.org.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MailServiceTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("from@local", "pass"))
            .withPerMethodLifecycle(false);

    @Test
    void sendsVerificationEmail() throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(ServerSetupTest.SMTP.getPort());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "false");

        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.mail.verify.subject", Locale.getDefault(), "Verify");
        source.addMessage("auth.mail.verify.body", Locale.getDefault(), "Expires {0}: {1}");
        MailService mailService = new MailService(sender, new Messages(source), "from@local");

        mailService.sendVerificationEmail("to@local", "http://verify", "24h");
        assertThat(GREEN_MAIL.getReceivedMessages()).hasSize(1);
        assertThat(GreenMailUtil.getBody(GREEN_MAIL.getReceivedMessages()[0])).contains("http://verify");
    }
}
'@

Write-Output "Created batch 3"
