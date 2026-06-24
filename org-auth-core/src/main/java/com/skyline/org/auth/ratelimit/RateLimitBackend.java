package com.skyline.org.auth.ratelimit;

public interface RateLimitBackend {

    boolean tryConsume(String key, int limitPerMinute);
}
