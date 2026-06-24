package com.skyline.org.auth.service;

import com.skyline.org.auth.dto.PasswordCheckResponse;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordCheckServiceTest {

    @Test
    void returnsLocalizedRules() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.password.valid", Locale.getDefault(), "ok");
        source.addMessage("auth.password.rule.length.pass", Locale.getDefault(), "len");
        source.addMessage("auth.password.rule.upper.pass", Locale.getDefault(), "up");
        source.addMessage("auth.password.rule.lower.pass", Locale.getDefault(), "lo");
        source.addMessage("auth.password.rule.digit.pass", Locale.getDefault(), "di");
        source.addMessage("auth.password.rule.special.pass", Locale.getDefault(), "sp");
        Messages messages = new Messages(source);
        PasswordCheckService service = new PasswordCheckService(messages);

        PasswordCheckResponse response = service.check("Str0ng!Pass");
        assertThat(response.valid()).isTrue();
        assertThat(response.rules()).isNotEmpty();
    }
}
