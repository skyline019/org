package com.skyline.org.auth.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameValidatorTest {

    @Test
    void validatesUsernameFormat() {
        assertThat(UsernameValidator.validateFormat("ab")).isPresent();
        assertThat(UsernameValidator.validateFormat("valid_user1")).isEmpty();
    }
}
