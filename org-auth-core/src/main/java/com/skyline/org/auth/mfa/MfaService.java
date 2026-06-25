package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.UserTotpCredential;
import com.skyline.org.auth.mfa.crypto.TotpSecretCryptoService;
import com.skyline.org.auth.repository.UserTotpRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@Service
public class MfaService {

    private final AuthProperties authProperties;
    private final UserTotpRepository userTotpRepository;
    private final UserService userService;
    private final TotpMfaService totpMfaService;
    private final MfaRecoveryCodeService mfaRecoveryCodeService;
    private final TotpSecretCryptoService totpSecretCryptoService;

    public MfaService(
            AuthProperties authProperties,
            UserTotpRepository userTotpRepository,
            UserService userService,
            TotpMfaService totpMfaService,
            MfaRecoveryCodeService mfaRecoveryCodeService,
            TotpSecretCryptoService totpSecretCryptoService) {
        this.authProperties = authProperties;
        this.userTotpRepository = userTotpRepository;
        this.userService = userService;
        this.totpMfaService = totpMfaService;
        this.mfaRecoveryCodeService = mfaRecoveryCodeService;
        this.totpSecretCryptoService = totpSecretCryptoService;
    }

    public boolean isFeatureEnabled() {
        return authProperties.getAuth().getMfa().isEnabled();
    }

    public boolean requiresChallenge(String username) {
        if (!isFeatureEnabled()) {
            return false;
        }
        return userTotpRepository.findByUsername(username)
                .map(UserTotpCredential::isEnabled)
                .orElse(false);
    }

    public boolean requiresMandatoryEnrollment(
            String username,
            Collection<? extends GrantedAuthority> authorities) {
        return hasMandatoryMfaRole(authorities) && !isEnrolled(username);
    }

    public boolean hasMandatoryMfaRole(Collection<? extends GrantedAuthority> authorities) {
        if (!isFeatureEnabled()) {
            return false;
        }
        var enforcedRoles = authProperties.getAuth().getMfa().getEnforceForRoles();
        if (enforcedRoles == null || enforcedRoles.isEmpty()) {
            return false;
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(enforcedRoles::contains);
    }

    public boolean isEnrolled(String username) {
        return userTotpRepository.findByUsername(username)
                .map(UserTotpCredential::isEnabled)
                .orElse(false);
    }

    @Transactional
    public String beginEnrollment(String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String secret = totpMfaService.generateSecret();
        UserTotpCredential credential = userTotpRepository.findByUsername(username)
                .orElseGet(() -> new UserTotpCredential(user, secret));
        credential.setSecret(totpSecretCryptoService.seal(secret));
        credential.setEnabled(false);
        userTotpRepository.save(credential);
        return secret;
    }

    @Transactional
    public MfaEnrollmentResult confirmEnrollment(String username, String code) {
        UserTotpCredential credential = requireCredential(username);
        if (!totpMfaService.verifyCode(resolvePlainSecret(credential), code)) {
            throw new IllegalArgumentException("Invalid verification code");
        }
        credential.setEnabled(true);
        userTotpRepository.save(credential);
        User user = userService.findByUsername(username).orElseThrow();
        return new MfaEnrollmentResult(mfaRecoveryCodeService.regenerateForUser(user));
    }

    public boolean verifyChallenge(String username, String code) {
        if (mfaRecoveryCodeService.tryConsume(username, code)) {
            return true;
        }
        return userTotpRepository.findByUsername(username)
                .filter(UserTotpCredential::isEnabled)
                .map(c -> totpMfaService.verifyCode(resolvePlainSecret(c), code))
                .orElse(false);
    }

    public String resolvePlainSecret(String username) {
        return userTotpRepository.findByUsername(username)
                .map(this::resolvePlainSecret)
                .orElseThrow(() -> new IllegalStateException("MFA enrollment not started"));
    }

    @Transactional
    public void disable(String username) {
        userTotpRepository.findByUsername(username).ifPresent(credential -> {
            credential.setEnabled(false);
            userTotpRepository.save(credential);
        });
        mfaRecoveryCodeService.deleteAllForUser(username);
    }

    public Optional<UserTotpCredential> findCredential(String username) {
        return userTotpRepository.findByUsername(username);
    }

    private String resolvePlainSecret(UserTotpCredential credential) {
        return totpSecretCryptoService.unseal(credential.getSecret());
    }

    private UserTotpCredential requireCredential(String username) {
        return userTotpRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("MFA enrollment not started"));
    }
}
