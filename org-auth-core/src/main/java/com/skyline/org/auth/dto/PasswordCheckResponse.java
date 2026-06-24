package com.skyline.org.auth.dto;

import java.util.List;

public record PasswordCheckResponse(
        boolean valid,
        int score,
        List<PasswordRuleItem> rules,
        String message
) {
}
