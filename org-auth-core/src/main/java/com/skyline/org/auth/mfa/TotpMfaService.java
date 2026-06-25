package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class TotpMfaService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final AuthProperties authProperties;

    public TotpMfaService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code.trim());
    }

    public String currentCodeForSecret(String secret) {
        try {
            return codeGenerator.generate(secret, timeProvider.getTime());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate TOTP code", ex);
        }
    }

    public String buildQrDataUri(String accountLabel, String secret) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(accountLabel)
                .secret(secret)
                .issuer(authProperties.getAuth().getMfa().getIssuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        byte[] image = qrGenerator.generate(data);
        return getDataUriForImage(image, qrGenerator.getImageMimeType());
    }
}
