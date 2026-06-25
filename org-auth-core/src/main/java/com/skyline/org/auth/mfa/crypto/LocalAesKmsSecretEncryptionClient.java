package com.skyline.org.auth.mfa.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM envelope encryption using a local master key.
 * Used for {@code local} mode and as the default {@code kms} provider in this starter.
 */
public class LocalAesKmsSecretEncryptionClient implements KmsSecretEncryptionClient {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public static LocalAesKmsSecretEncryptionClient fromRawKey(byte[] rawKey) {
        if (rawKey == null || rawKey.length != 32) {
            throw new IllegalArgumentException("TOTP encryption key must be 32 bytes (256 bits)");
        }
        return new LocalAesKmsSecretEncryptionClient(rawKey);
    }

    private LocalAesKmsSecretEncryptionClient(byte[] rawKey) {
        this.secretKey = new SecretKeySpec(rawKey, "AES");
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return payload;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt TOTP secret", ex);
        }
    }

    @Override
    public byte[] decrypt(byte[] payload) {
        if (payload == null || payload.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid encrypted TOTP payload");
        }
        try {
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to decrypt TOTP secret", ex);
        }
    }
}
