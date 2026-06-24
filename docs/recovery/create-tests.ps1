$utf8 = New-Object System.Text.UTF8Encoding $false
$base = "e:\JavaProjects\org\src\test\java"

function Write-TestFile($relPath, $content) {
    $full = Join-Path $base $relPath
    $dir = Split-Path $full -Parent
    [System.IO.Directory]::CreateDirectory($dir) | Out-Null
    [System.IO.File]::WriteAllText($full, $content.TrimStart() + "`n", $utf8)
}

Write-TestFile "com/skyline/org/testsupport/AbstractIntegrationTest.java" @'
package com.skyline.org.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("org_auth_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
    }
}
'@

Write-TestFile "com/skyline/org/testsupport/MailIntegrationSupport.java" @'
package com.skyline.org.testsupport;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class MailIntegrationSupport extends AbstractIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test@local", "test"))
            .withPerMethodLifecycle(false);

    @DynamicPropertySource
    static void configureMail(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
        registry.add("spring.mail.username", () -> "");
        registry.add("spring.mail.password", () -> "");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
    }

    protected static String unique(String prefix) {
        return prefix + System.nanoTime();
    }
}
'@

Write-TestFile "com/skyline/org/OrgApplicationTests.java" @'
package com.skyline.org;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class OrgApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
'@

Write-TestFile "com/skyline/org/common/util/TokenGeneratorTest.java" @'
package com.skyline.org.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    @Test
    void generatesUniqueTokensAndStableHash() {
        String raw = TokenGenerator.generateRawToken();
        String raw2 = TokenGenerator.generateRawToken();
        assertThat(raw).isNotEqualTo(raw2);
        assertThat(TokenGenerator.hashToken(raw)).hasSize(64);
        assertThat(TokenGenerator.constantTimeEquals(TokenGenerator.hashToken(raw), TokenGenerator.hashToken(raw))).isTrue();
        assertThat(TokenGenerator.constantTimeEquals(TokenGenerator.hashToken(raw), TokenGenerator.hashToken(raw2))).isFalse();
    }
}
'@

Write-TestFile "com/skyline/org/common/web/ClientIpResolverTest.java" @'
package com.skyline.org.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void resolvesForwardedClientIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("127.0.0.1");
    }
}
'@

Write-TestFile "com/skyline/org/auth/validation/PasswordStrengthCheckerTest.java" @'
package com.skyline.org.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordStrengthCheckerTest {

    @Test
    void rejectsWeakPassword() {
        PasswordStrengthResult result = PasswordStrengthChecker.check("weak");
        assertThat(result.valid()).isFalse();
    }

    @Test
    void acceptsStrongPassword() {
        PasswordStrengthResult result = PasswordStrengthChecker.check("Str0ng!Pass");
        assertThat(result.valid()).isTrue();
        assertThat(result.score()).isGreaterThanOrEqualTo(100);
    }
}
'@

Write-TestFile "com/skyline/org/auth/validation/EmailValidatorTest.java" @'
package com.skyline.org.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidatorTest {

    @Test
    void validatesEmailFormat() {
        assertThat(EmailValidator.validateFormat("bad")).isPresent();
        assertThat(EmailValidator.validateFormat("user@example.com")).isEmpty();
    }
}
'@

Write-TestFile "com/skyline/org/auth/validation/UsernameValidatorTest.java" @'
package com.skyline.org.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameValidatorTest {

    @Test
    void validatesUsernameFormat() {
        assertThat(UsernameValidator.validateFormat("ab")).isPresent();
        assertThat(UsernameValidator.validateFormat("valid_user1")).isEmpty();
    }
}
'@

Write-Output "Created batch 1"
