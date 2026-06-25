package com.skyline.org.auth.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryRateLimitBackendTest {

    @Test
    void allowsRequestsWithinLimit() {
        MemoryRateLimitBackend backend = new MemoryRateLimitBackend();
        assertThat(backend.tryConsume("login:127.0.0.1", 2)).isTrue();
        assertThat(backend.tryConsume("login:127.0.0.1", 2)).isTrue();
        assertThat(backend.tryConsume("login:127.0.0.1", 2)).isFalse();
    }

    @Test
    void isolatesKeys() {
        MemoryRateLimitBackend backend = new MemoryRateLimitBackend();
        assertThat(backend.tryConsume("login:1.1.1.1", 1)).isTrue();
        assertThat(backend.tryConsume("login:2.2.2.2", 1)).isTrue();
    }
}
