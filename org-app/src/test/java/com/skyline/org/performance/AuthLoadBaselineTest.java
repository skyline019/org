package com.skyline.org.performance;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lightweight latency baseline for critical read-only auth endpoints (not a full load test).
 * See docs/PERFORMANCE.md for interpretation.
 */
@AutoConfigureMockMvc
class AuthLoadBaselineTest extends AbstractIntegrationTest {

    private static final int SAMPLES = 100;
    private static final long P95_LIMIT_MS = 800;

    @Autowired MockMvc mockMvc;

    @Test
    void usernameCheckApiP95UnderBaseline() throws Exception {
        List<Long> samples = new ArrayList<>(SAMPLES);
        for (int i = 0; i < SAMPLES; i++) {
            long begin = System.nanoTime();
            mockMvc.perform(get("/api/v1/auth/check/username").param("value", "user_" + i))
                    .andExpect(status().isOk());
            samples.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
        }

        Collections.sort(samples);
        long p95 = samples.get((int) Math.ceil(samples.size() * 0.95) - 1);
        assertThat(p95).as("p95 latency ms for /api/v1/auth/check/username").isLessThan(P95_LIMIT_MS);
    }
}
