package com.skyline.org.auth.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.auth.rate-limit.backend", havingValue = "memory", matchIfMissing = true)
public class MemoryRateLimitBackend implements RateLimitBackend {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    @Override
    public boolean tryConsume(String key, int limitPerMinute) {
        Bucket bucket = buckets.get(key, k -> createBucket(limitPerMinute));
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(int perMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
