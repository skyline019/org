package com.skyline.org.auth.mfa.crypto;

import com.skyline.org.auth.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpSecretCryptoServiceTest {

    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    AuthProperties authProperties;
    TotpSecretCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.getAuth().getMfa().getSecretEncryption().setMode("local");
        authProperties.getAuth().getMfa().getSecretEncryption().setLocalKey(TEST_KEY);
        cryptoService = new TotpSecretCryptoService(
                authProperties,
                LocalAesKmsSecretEncryptionClient.fromRawKey(TotpSecretCryptoConfiguration.decodeKey(TEST_KEY)));
    }

    @Test
    void sealAndUnsealRoundTrip() {
        String sealed = cryptoService.seal("JBSWY3DPEHPK3PXP");

        assertThat(sealed).startsWith(TotpSecretCryptoService.SEALED_PREFIX);
        assertThat(cryptoService.unseal(sealed)).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    void unsealSupportsLegacyPlaintext() {
        assertThat(cryptoService.unseal("JBSWY3DPEHPK3PXP")).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(cryptoService.isSealed("JBSWY3DPEHPK3PXP")).isFalse();
    }

    @Test
    void disabledModePassesThrough() {
        authProperties.getAuth().getMfa().getSecretEncryption().setMode("none");
        TotpSecretCryptoService disabled = new TotpSecretCryptoService(
                authProperties,
                new KmsSecretEncryptionClient() {
                    @Override
                    public byte[] encrypt(byte[] plaintext) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public byte[] decrypt(byte[] ciphertext) {
                        throw new UnsupportedOperationException();
                    }
                });

        assertThat(disabled.seal("plain")).isEqualTo("plain");
        assertThat(disabled.isEncryptionEnabled()).isFalse();
    }

    @Test
    void rejectsInvalidKeyLength() {
        assertThatThrownBy(() -> TotpSecretCryptoConfiguration.decodeKey(
                Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalStateException.class);
    }
}
