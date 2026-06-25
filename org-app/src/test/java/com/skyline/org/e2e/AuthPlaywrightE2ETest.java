package com.skyline.org.e2e;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.MailIntegrationSupport;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthPlaywrightE2ETest extends MailIntegrationSupport {

    @LocalServerPort
    int port;

    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService emailVerificationService;

    private String username;
    private String password;

    @BeforeEach
    void registerVerifiedUser() throws Exception {
        username = unique("e2e");
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
    void loginPageShowsFormFields() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(baseUrl() + "/login");
            assertThat(page.locator("#username").isVisible()).isTrue();
            assertThat(page.locator("#password").isVisible()).isTrue();
            assertThat(page.locator("button[type='submit']").isVisible()).isTrue();
            browser.close();
        }
    }

    @Test
    void registerPageShowsFormFields() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(baseUrl() + "/register");
            assertThat(page.locator("#username").isVisible()).isTrue();
            assertThat(page.locator("#email").isVisible()).isTrue();
            assertThat(page.locator("#password").isVisible()).isTrue();
            browser.close();
        }
    }

    @Test
    void successfulLoginRedirectsToHome() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(baseUrl() + "/login");
            page.fill("#username", username);
            page.fill("#password", password);
            page.locator("button[type='submit']").click();
            page.waitForURL("**/home");
            assertThat(page.url()).endsWith("/home");
            browser.close();
        }
    }

    @Test
    void unauthenticatedHomeRedirectsToLogin() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(baseUrl() + "/home");
            page.waitForURL("**/login**");
            assertThat(page.url()).contains("/login");
            browser.close();
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
