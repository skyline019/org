package com.skyline.org.auth.validation;

import java.util.Optional;
import java.util.regex.Pattern;

public final class EmailValidator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private EmailValidator() {
    }

    /** Returns a message key if format is invalid. */
    public static Optional<String> validateFormat(String email) {
        if (email == null || email.isBlank()) {
            return Optional.of("auth.validation.email.blank");
        }
        if (!EMAIL.matcher(email).matches()) {
            return Optional.of("auth.validation.email.format");
        }
        return Optional.empty();
    }
}
