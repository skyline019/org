package com.skyline.org.auth.validation;

import java.util.List;

public record PasswordStrengthResult(boolean valid, int score, List<String> rules, String message) {
}
