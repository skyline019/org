package com.skyline.org.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginPagePlaywrightE2ETest {

    @LocalServerPort
    int port;

    @Test
    void loginPageShowsUsernameAndPasswordFields() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate("http://localhost:" + port + "/login");
            assertThat(page.locator("#username").isVisible()).isTrue();
            assertThat(page.locator("#password").isVisible()).isTrue();
            assertThat(page.locator("button[type='submit']").isVisible()).isTrue();
            browser.close();
        }
    }
}
