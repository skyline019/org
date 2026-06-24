package com.skyline.org.auth.service;

import com.skyline.org.auth.dto.PasswordCheckResponse;
import com.skyline.org.auth.dto.PasswordRuleItem;
import com.skyline.org.auth.validation.PasswordStrengthChecker;
import com.skyline.org.auth.validation.PasswordStrengthResult;
import com.skyline.org.common.i18n.Messages;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PasswordCheckService {

    private final Messages messages;

    public PasswordCheckService(Messages messages) {
        this.messages = messages;
    }

    public PasswordCheckResponse check(String password) {
        PasswordStrengthResult result = PasswordStrengthChecker.check(password);
        List<PasswordRuleItem> rules = result.rules().stream()
                .map(key -> new PasswordRuleItem(messages.get(key), key.endsWith(".pass")))
                .toList();
        return new PasswordCheckResponse(
                result.valid(),
                result.score(),
                rules,
                messages.get(result.message()));
    }
}
