package com.skyline.org.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MfaPlaywrightE2ETest extends MailIntegrationSupport {

    private static final String TEST_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @LocalServerPort
    int port;

    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired TotpMfaService totpMfaService;

    private String username;
    private String password;

    @DynamicPropertySource
    static void enableMfa(DynamicPropertyRegistry registry) {
        registry.add("app.auth.mfa.enabled", () -> "true");
        registry.add("app.auth.mfa.secret-encryption.mode", () -> "local");
        registry.add("app.auth.mfa.secret-encryption.local-key", () -> TEST_ENCRYPTION_KEY);
    }

    @BeforeEach
    void registerVerifiedUser() throws Exception {
        username = unique("mfae2e");
        password = "Str0ng!Pass";
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(username + "@example.com");
        request.setPassword(password);
        request.setConfirmPassword(password);
        registrationService.register(request);
        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(GREEN_MAIL.getReceivedMessages()).isNotEmpty());
        String body = GREEN_MAIL.getReceivedMessages()[GREEN_MAIL.getReceivedMessages().length - 1].getContent().toString();
        Matcher matcher = Pattern.compile("/auth/verify-email/([A-Za-z0-9_-]+)").matcher(body);
        assertThat(matcher.find()).isTrue();
        emailVerificationService.verifyEmail(matcher.group(1));
    }

    @Test
    void enrollMfaThenLoginRequiresChallenge() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            login(page);
            String secret = enrollMfa(page);

            page.locator("a[href*='home']").click();
            page.waitForURL("**/home");

            logout(page);
            loginExpectingChallenge(page);

            page.fill("form[action$='/auth/mfa/challenge'] #code", totpMfaService.currentCodeForSecret(secret));
            page.locator("form[action$='/auth/mfa/challenge'] button[type='submit']").click();
            page.waitForURL("**/home");
            assertThat(page.url()).endsWith("/home");

            browser.close();
        }
    }

    @Test
    void loginWithRecoveryCodeWhenAuthenticatorLost() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            login(page);
            enrollMfa(page);
            String recoveryCode = page.locator(".mfa-recovery-list li code").first().innerText().trim();
            assertThat(recoveryCode).isNotBlank();

            page.locator("a[href*='home']").click();
            page.waitForURL("**/home");

            logout(page);
            loginExpectingChallenge(page);

            page.fill("form[action$='/auth/mfa/challenge'] #code", recoveryCode);
            page.locator("form[action$='/auth/mfa/challenge'] button[type='submit']").click();
            page.waitForURL("**/home");
            assertThat(page.url()).endsWith("/home");

            browser.close();
        }
    }

    private void login(Page page) {
        loginExpecting(page, "**/home");
    }

    private void loginExpectingChallenge(Page page) {
        loginExpecting(page, "**/auth/mfa/challenge**");
    }

    private void loginExpecting(Page page, String urlPattern) {
        page.navigate(baseUrl() + "/login");
        page.fill("#username", username);
        page.fill("#password", password);
        page.locator("button[type='submit']").click();
        page.waitForURL(urlPattern);
    }

    private String enrollMfa(Page page) {
        page.navigate(baseUrl() + "/auth/mfa/setup");
        page.waitForSelector(".mfa-secret code");
        String secret = page.locator(".mfa-secret code").innerText().trim();
        page.fill("form[action$='/auth/mfa/enable'] #code", totpMfaService.currentCodeForSecret(secret));
        page.locator("form[action$='/auth/mfa/enable'] button[type='submit']").click();
        page.waitForURL("**/auth/mfa/recovery-codes**");
        assertThat(page.locator(".mfa-recovery-list li").count()).isGreaterThan(0);
        return secret;
    }

    private void logout(Page page) {
        page.locator("section.home-page form.inline-form button.btn-secondary").click();
        page.waitForURL("**/login**");
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
