package com.skyline.org.auth.mfa.crypto;

/**
 * Pluggable backend for envelope encryption of TOTP secrets.
 * Production can replace this bean with an AWS KMS / Vault / HSM client.
 */
public interface KmsSecretEncryptionClient {

    byte[] encrypt(byte[] plaintext);

    byte[] decrypt(byte[] ciphertext);
}
