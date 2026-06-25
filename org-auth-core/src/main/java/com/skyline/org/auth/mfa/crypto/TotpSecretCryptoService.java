package com.skyline.org.auth.mfa.crypto;

import com.skyline.org.auth.config.AuthProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Seals and unseals TOTP shared secrets at rest.
 * Legacy plaintext values (no {@code v1:} prefix) remain readable for migration.
 */
public class TotpSecretCryptoService {

    static final String SEALED_PREFIX = "v1:";

    private final AuthProperties authProperties;
    private final KmsSecretEncryptionClient encryptionClient;

    public TotpSecretCryptoService(AuthProperties authProperties, KmsSecretEncryptionClient encryptionClient) {
        this.authProperties = authProperties;
        this.encryptionClient = encryptionClient;
    }

    public boolean isEncryptionEnabled() {
        String mode = authProperties.getAuth().getMfa().getSecretEncryption().getMode();
        return mode != null && !"none".equalsIgnoreCase(mode.trim());
    }

    public String seal(String plainSecret) {
        if (plainSecret == null || plainSecret.isBlank() || !isEncryptionEnabled()) {
            return plainSecret;
        }
        byte[] ciphertext = encryptionClient.encrypt(plainSecret.getBytes(StandardCharsets.UTF_8));
        return SEALED_PREFIX + Base64.getEncoder().encodeToString(ciphertext);
    }

    public String unseal(String storedSecret) {
        if (storedSecret == null || storedSecret.isBlank()) {
            return storedSecret;
        }
        if (!storedSecret.startsWith(SEALED_PREFIX)) {
            return storedSecret;
        }
        byte[] payload = Base64.getDecoder().decode(storedSecret.substring(SEALED_PREFIX.length()));
        return new String(encryptionClient.decrypt(payload), StandardCharsets.UTF_8);
    }

    public boolean isSealed(String storedSecret) {
        return storedSecret != null && storedSecret.startsWith(SEALED_PREFIX);
    }
}
