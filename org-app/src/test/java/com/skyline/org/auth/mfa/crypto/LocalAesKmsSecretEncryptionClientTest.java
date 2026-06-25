package com.skyline.org.auth.mfa.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAesKmsSecretEncryptionClientTest {

    private static final byte[] KEY = new byte[32];

    @Test
    void encryptDecryptRoundTrip() {
        LocalAesKmsSecretEncryptionClient client = LocalAesKmsSecretEncryptionClient.fromRawKey(KEY);
        byte[] plain = "totp-secret-value".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = client.encrypt(plain);
        byte[] decrypted = client.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plain);
        assertThat(encrypted).isNotEqualTo(plain);
    }

    @Test
    void acceptsBase64KeyFromConfigurationHelper() {
        String encoded = Base64.getEncoder().encodeToString(KEY);
        LocalAesKmsSecretEncryptionClient client = LocalAesKmsSecretEncryptionClient.fromRawKey(
                TotpSecretCryptoConfiguration.decodeKey(encoded));

        assertThat(client.decrypt(client.encrypt(new byte[]{1, 2, 3}))).containsExactly(1, 2, 3);
    }
}
