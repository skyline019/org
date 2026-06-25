package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.UserTotpCredential;
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

    public MfaService(
            AuthProperties authProperties,
            UserTotpRepository userTotpRepository,
            UserService userService,
            TotpMfaService totpMfaService,
            MfaRecoveryCodeService mfaRecoveryCodeService) {
        this.authProperties = authProperties;
        this.userTotpRepository = userTotpRepository;
        this.userService = userService;
        this.totpMfaService = totpMfaService;
        this.mfaRecoveryCodeService = mfaRecoveryCodeService;
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
        if (!isFeatureEnabled() || isEnrolled(username)) {
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
        credential.setSecret(secret);
        credential.setEnabled(false);
        userTotpRepository.save(credential);
        return secret;
    }

    @Transactional
    public MfaEnrollmentResult confirmEnrollment(String username, String code) {
        UserTotpCredential credential = requireCredential(username);
        if (!totpMfaService.verifyCode(credential.getSecret(), code)) {
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
                .map(c -> totpMfaService.verifyCode(c.getSecret(), code))
                .orElse(false);
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

    private UserTotpCredential requireCredential(String username) {
        return userTotpRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("MFA enrollment not started"));
    }
}
