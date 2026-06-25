package com.skyline.org.auth.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisRateLimitBackendTest {

    @Test
    @SuppressWarnings("unchecked")
    void consumesWithinLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L, 1L, 0L);

        RedisRateLimitBackend backend = new RedisRateLimitBackend(redis);
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isTrue();
        assertThat(backend.tryConsume("login:1.2.3.4", 3)).isFalse();
    }
}
