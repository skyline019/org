package com.skyline.org.integration;

import com.skyline.org.auth.ratelimit.RateLimitBackend;
import com.skyline.org.auth.ratelimit.RateLimitService;
import com.skyline.org.auth.ratelimit.RedisRateLimitBackend;
import com.skyline.org.testsupport.RedisIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIf("com.skyline.org.testsupport.RedisAvailableCondition#isAvailable")
@ActiveProfiles({"test", "redis"})
class RedisRateLimitIntegrationTest extends RedisIntegrationSupport {

    @Autowired RateLimitBackend rateLimitBackend;
    @Autowired RateLimitService rateLimitService;

    @Test
    void wiresRedisBackend() {
        assertThat(rateLimitBackend).isInstanceOf(RedisRateLimitBackend.class);
    }

    @Test
    void enforcesLimitAcrossCalls() {
        String ip = "203.0.113.10";
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimitService.tryConsume("/login", "POST", ip)).isTrue();
        }
        assertThat(rateLimitService.tryConsume("/login", "POST", ip)).isFalse();
    }
}
