package com.skyline.org.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenGeneratorTest {

    @Test
    void generatesUniqueTokensAndStableHash() {
        String raw = TokenGenerator.generateRawToken();
        String raw2 = TokenGenerator.generateRawToken();
        assertThat(raw).isNotEqualTo(raw2);
        assertThat(TokenGenerator.hashToken(raw)).hasSize(64);
        assertThat(TokenGenerator.constantTimeEquals(TokenGenerator.hashToken(raw), TokenGenerator.hashToken(raw))).isTrue();
        assertThat(TokenGenerator.constantTimeEquals(TokenGenerator.hashToken(raw), TokenGenerator.hashToken(raw2))).isFalse();
    }
}
