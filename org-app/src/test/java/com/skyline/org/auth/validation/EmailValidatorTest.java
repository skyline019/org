package com.skyline.org.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailValidatorTest {

    @Test
    void validatesEmailFormat() {
        assertThat(EmailValidator.validateFormat("bad")).isPresent();
        assertThat(EmailValidator.validateFormat("user@example.com")).isEmpty();
    }
}
