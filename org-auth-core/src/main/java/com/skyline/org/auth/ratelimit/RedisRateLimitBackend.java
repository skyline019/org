package com.skyline.org.auth.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@ConditionalOnProperty(name = "app.auth.rate-limit.backend", havingValue = "redis")
public class RedisRateLimitBackend implements RateLimitBackend {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final long REFILL_PERIOD_MS = 60_000L;

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>("""
            local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
            local last_refill = tonumber(redis.call('HGET', KEYS[1], 'last_refill'))
            local capacity = tonumber(ARGV[1])
            local now = tonumber(ARGV[2])
            local period = tonumber(ARGV[3])
            if tokens == nil then
              tokens = capacity
              last_refill = now
            end
            local elapsed = now - last_refill
            if elapsed > 0 then
              local refill = math.floor(elapsed * capacity / period)
              if refill > 0 then
                tokens = math.min(capacity, tokens + refill)
                last_refill = now
              end
            end
            if tokens < 1 then
              redis.call('HSET', KEYS[1], 'tokens', tokens, 'last_refill', last_refill)
              redis.call('PEXPIRE', KEYS[1], period * 2)
              return 0
            end
            tokens = tokens - 1
            redis.call('HSET', KEYS[1], 'tokens', tokens, 'last_refill', last_refill)
            redis.call('PEXPIRE', KEYS[1], period * 2)
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitBackend(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryConsume(String key, int limitPerMinute) {
        String redisKey = KEY_PREFIX + key;
        Long allowed = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                Collections.singletonList(redisKey),
                String.valueOf(limitPerMinute),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(REFILL_PERIOD_MS));
        return allowed != null && allowed == 1L;
    }
}
