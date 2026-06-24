package com.skyline.org.auth.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitBackendTest {

    @Test
    void consumesWithinLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);
        when(redis.expire(anyString(), any(java.time.Duration.class))).thenReturn(true);

        RedisRateLimitBackend backend = new RedisRateLimitBackend(redis);
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
    }

    @Test
    void rejectsOverLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(4L);

        RedisRateLimitBackend backend = new RedisRateLimitBackend(redis);
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isFalse();
    }
}
