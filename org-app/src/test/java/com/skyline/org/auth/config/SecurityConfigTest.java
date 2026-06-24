package com.skyline.org.auth.config;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired SecurityFilterChain securityFilterChain;

    @Test
    void securityFilterChainIsConfigured() {
        assertThat(securityFilterChain).isNotNull();
    }
}
