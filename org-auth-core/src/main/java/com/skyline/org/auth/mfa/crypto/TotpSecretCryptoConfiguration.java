package com.skyline.org.auth.mfa.crypto;

import com.skyline.org.auth.config.AuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class TotpSecretCryptoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KmsSecretEncryptionClient kmsSecretEncryptionClient(AuthProperties authProperties) {
        AuthProperties.Mfa.SecretEncryption encryption = authProperties.getAuth().getMfa().getSecretEncryption();
        String mode = encryption.getMode() == null ? "none" : encryption.getMode().trim();
        if ("none".equalsIgnoreCase(mode)) {
            return new NoOpKmsSecretEncryptionClient();
        }
        byte[] key = decodeKey(encryption.getLocalKey());
        return LocalAesKmsSecretEncryptionClient.fromRawKey(key);
    }

    @Bean
    public TotpSecretCryptoService totpSecretCryptoService(
            AuthProperties authProperties,
            KmsSecretEncryptionClient kmsSecretEncryptionClient) {
        return new TotpSecretCryptoService(authProperties, kmsSecretEncryptionClient);
    }

    static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "app.auth.mfa.secret-encryption.local-key is required when secret encryption is enabled");
        }
        byte[] decoded = Base64.getDecoder().decode(base64Key.trim());
        if (decoded.length != 32) {
            throw new IllegalStateException("TOTP encryption key must decode to 32 bytes");
        }
        return decoded;
    }

    private static final class NoOpKmsSecretEncryptionClient implements KmsSecretEncryptionClient {
        @Override
        public byte[] encrypt(byte[] plaintext) {
            throw new IllegalStateException("TOTP secret encryption is disabled");
        }

        @Override
        public byte[] decrypt(byte[] ciphertext) {
            throw new IllegalStateException("TOTP secret encryption is disabled");
        }
    }
}
