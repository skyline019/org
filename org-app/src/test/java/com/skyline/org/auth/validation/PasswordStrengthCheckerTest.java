package com.skyline.org.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordStrengthCheckerTest {

    @Test
    void rejectsWeakPassword() {
        PasswordStrengthResult result = PasswordStrengthChecker.check("weak");
        assertThat(result.valid()).isFalse();
    }

    @Test
    void acceptsStrongPassword() {
        PasswordStrengthResult result = PasswordStrengthChecker.check("Str0ng!Pass");
        assertThat(result.valid()).isTrue();
        assertThat(result.score()).isGreaterThanOrEqualTo(100);
    }
}
