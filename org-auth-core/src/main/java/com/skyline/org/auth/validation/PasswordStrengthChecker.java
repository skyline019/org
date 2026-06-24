package com.skyline.org.auth.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-pass password strength analysis — O(n) time, O(1) extra space.
 * Returns message keys (not localized text); resolve via {@link com.skyline.org.auth.service.PasswordCheckService}.
 */
public final class PasswordStrengthChecker {

    private static final int MIN_LENGTH = 8;

    private PasswordStrengthChecker() {
    }

    public static PasswordStrengthResult check(String password) {
        if (password == null || password.isBlank()) {
            return new PasswordStrengthResult(false, 0, List.of(), "auth.password.empty");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        List<String> rules = new ArrayList<>(5);
        int score = 0;

        if (password.length() >= MIN_LENGTH) {
            score += 25;
            rules.add("auth.password.rule.length.pass");
        } else {
            rules.add("auth.password.rule.length.fail");
        }

        score += addRule(hasUpper, 25, "auth.password.rule.upper", rules);
        score += addRule(hasLower, 25, "auth.password.rule.lower", rules);
        score += addRule(hasDigit, 15, "auth.password.rule.digit", rules);
        score += addRule(hasSpecial, 10, "auth.password.rule.special", rules);

        boolean valid = password.length() >= MIN_LENGTH && hasUpper && hasLower && hasDigit && hasSpecial;
        String messageKey = valid ? "auth.password.valid" : "auth.password.invalid";
        return new PasswordStrengthResult(valid, score, rules, messageKey);
    }

    private static int addRule(boolean passed, int points, String keyPrefix, List<String> rules) {
        if (passed) {
            rules.add(keyPrefix + ".pass");
            return points;
        }
        rules.add(keyPrefix + ".fail");
        return 0;
    }
}
