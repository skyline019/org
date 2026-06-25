package com.skyline.org.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class RedisIntegrationSupport extends AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> System.getenv().getOrDefault("REDIS_HOST", "localhost"));
        registry.add("spring.data.redis.port", () -> System.getenv().getOrDefault("REDIS_PORT", "6379"));
        registry.add("spring.autoconfigure.exclude", () -> "");
        registry.add("app.auth.rate-limit.backend", () -> "redis");
        registry.add("management.health.redis.enabled", () -> "true");
    }
}
