package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpMfaServiceTest {

    TotpMfaService totpMfaService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.getAuth().getMfa().setIssuer("Org Auth Test");
        totpMfaService = new TotpMfaService(properties);
    }

    @Test
    void verifyCodeRejectsBlankInputs() {
        assertThat(totpMfaService.verifyCode(null, "123456")).isFalse();
        assertThat(totpMfaService.verifyCode("secret", null)).isFalse();
        assertThat(totpMfaService.verifyCode(" ", "123456")).isFalse();
        assertThat(totpMfaService.verifyCode("secret", " ")).isFalse();
    }

    @Test
    void generateSecretProducesNonBlankValue() {
        assertThat(totpMfaService.generateSecret()).isNotBlank();
    }

    @Test
    void currentCodeForSecretProducesSixDigits() {
        String secret = totpMfaService.generateSecret();

        assertThat(totpMfaService.currentCodeForSecret(secret)).matches("\\d{6}");
    }

    @Test
    void currentCodeForSecretIsAcceptedByVerifyCode() {
        String secret = totpMfaService.generateSecret();
        String code = totpMfaService.currentCodeForSecret(secret);

        assertThat(totpMfaService.verifyCode(secret, code)).isTrue();
    }

    @Test
    void buildQrDataUriReturnsPngDataUri() throws Exception {
        String secret = totpMfaService.generateSecret();

        String dataUri = totpMfaService.buildQrDataUri("alice@example.com", secret);

        assertThat(dataUri).startsWith("data:image/png;base64,");
    }
}
