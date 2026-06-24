package com.skyline.org.demo;

import com.skyline.org.auth.security.OrgAuthSecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoSecurityCustomizer {

    @Bean
    OrgAuthSecurityCustomizer dashboardSecurity() {
        return auth -> auth.requestMatchers("/dashboard", "/dashboard/**").authenticated();
    }
}
