package com.skyline.org.auth.validation;

import java.util.Optional;

public final class UsernameValidator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    private UsernameValidator() {
    }

    /** Returns a message key if format is invalid. */
    public static Optional<String> validateFormat(String username) {
        if (username == null || username.isBlank()) {
            return Optional.of("auth.validation.username.blank");
        }
        if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
            return Optional.of("auth.validation.username.length");
        }
        for (int i = 0; i < username.length(); i++) {
            char c = username.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return Optional.of("auth.validation.username.format");
            }
        }
        return Optional.empty();
    }
}
