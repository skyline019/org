package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.UserMfaRecoveryCode;
import com.skyline.org.auth.repository.UserMfaRecoveryCodeRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class MfaRecoveryCodeService {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final UserMfaRecoveryCodeRepository recoveryCodeRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public MfaRecoveryCodeService(
            UserMfaRecoveryCodeRepository recoveryCodeRepository,
            UserService userService,
            PasswordEncoder passwordEncoder,
            AuthProperties authProperties) {
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public List<String> regenerateForUser(User user) {
        recoveryCodeRepository.deleteByUserId(user.getId());
        int count = authProperties.getAuth().getMfa().getRecoveryCodeCount();
        List<String> plainCodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String plain = formatPlainCode();
            plainCodes.add(plain);
            recoveryCodeRepository.save(new UserMfaRecoveryCode(user, passwordEncoder.encode(normalize(plain))));
        }
        return plainCodes;
    }

    @Transactional
    public boolean tryConsume(String username, String rawCode) {
        if (!looksLikeRecoveryCode(rawCode)) {
            return false;
        }
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }
        String normalized = normalize(rawCode);
        for (UserMfaRecoveryCode stored : recoveryCodeRepository.findUnusedByUserId(user.getId())) {
            if (passwordEncoder.matches(normalized, stored.getCodeHash())) {
                stored.setUsedAt(Instant.now());
                recoveryCodeRepository.save(stored);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void deleteAllForUser(String username) {
        userService.findByUsername(username).ifPresent(user -> recoveryCodeRepository.deleteByUserId(user.getId()));
    }

    public long countUnused(String username) {
        return userService.findByUsername(username)
                .map(user -> recoveryCodeRepository.countByUserIdAndUsedAtIsNull(user.getId()))
                .orElse(0L);
    }

    public static boolean looksLikeRecoveryCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return false;
        }
        String normalized = normalize(rawCode);
        return normalized.length() >= 8 && !normalized.matches("\\d{6}");
    }

    static String normalize(String rawCode) {
        return rawCode.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private static String formatPlainCode() {
        return randomBlock(4) + "-" + randomBlock(4);
    }

    private static String randomBlock(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHANUM.charAt(RNG.nextInt(ALPHANUM.length())));
        }
        return builder.toString();
    }
}
