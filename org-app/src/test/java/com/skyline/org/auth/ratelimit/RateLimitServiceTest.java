package com.skyline.org.auth.ratelimit;

import com.skyline.org.auth.config.AuthProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    @Test
    void allowsUnlimitedPaths() {
        AuthProperties props = new AuthProperties();
        RateLimitService service = new RateLimitService(props, (key, limit) -> true);
        assertThat(service.tryConsume("/home", "GET", "1.2.3.4")).isTrue();
    }

    @Test
    void delegatesLimitedPaths() {
        AuthProperties props = new AuthProperties();
        props.getAuth().getRateLimit().setLoginPerMinute(2);
        RateLimitService service = new RateLimitService(props, (key, limit) -> limit == 2);
        assertThat(service.tryConsume("/login", "POST", "1.2.3.4")).isTrue();
    }
}
