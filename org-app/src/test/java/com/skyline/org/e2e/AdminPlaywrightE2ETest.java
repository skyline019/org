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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminPlaywrightE2ETest {

    @LocalServerPort
    int port;

    @DynamicPropertySource
    static void bootstrapAdmin(DynamicPropertyRegistry registry) {
        registry.add("app.auth.bootstrap-admin.enabled", () -> "true");
        registry.add("app.auth.bootstrap-admin.username", () -> "e2eadmin");
        registry.add("app.auth.bootstrap-admin.email", () -> "e2eadmin@example.com");
        registry.add("app.auth.bootstrap-admin.password", () -> "Admin!Pass1");
    }

    @Test
    void adminCanOpenUserManagementPage() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(baseUrl() + "/login");
            page.fill("#username", "e2eadmin");
            page.fill("#password", "Admin!Pass1");
            page.locator("button[type='submit']").click();
            page.waitForURL("**/home");
            page.navigate(baseUrl() + "/admin/users");
            assertThat(page.locator("table").isVisible()).isTrue();
            browser.close();
        }
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
