package com.skyline.org.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class DemoBusinessApplicationTest {

    @Autowired
    MockMvc mockMvc;

    @DynamicPropertySource
    static void sharedLocalSecrets(DynamicPropertyRegistry registry) {
        loadPropertiesFrom(findSharedDevLocalFile()).forEach((key, value) ->
                registry.add(key.toString(), () -> value.toString()));
        String envPassword = System.getenv("DB_PASSWORD");
        if (envPassword != null && !envPassword.isBlank()) {
            registry.add("spring.datasource.password", () -> envPassword);
        }
    }

    @Test
    void contextLoads() {
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    private static Path findSharedDevLocalFile() {
        Path[] candidates = {
                Path.of("org-app/src/main/resources/application-dev.local.properties"),
                Path.of("../org-app/src/main/resources/application-dev.local.properties")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static Properties loadPropertiesFrom(Path path) {
        Properties properties = new Properties();
        if (path == null) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException ignored) {
            // optional local file
        }
        return properties;
    }
}
