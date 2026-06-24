package com.skyline.org.autoconfigure;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.config.PasswordEncoderConfig;
import com.skyline.org.config.FlywayMigrationConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@EnableConfigurationProperties(AuthProperties.class)
@EntityScan(basePackages = {
        "com.skyline.org.user.entity",
        "com.skyline.org.auth.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.skyline.org.user.repository",
        "com.skyline.org.auth.repository"
})
@ComponentScan(basePackages = {
        "com.skyline.org.common",
        "com.skyline.org.user",
        "com.skyline.org.mail",
        "com.skyline.org.auth"
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.skyline\\.org\\.auth\\.controller\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.skyline\\.org\\.auth\\.security\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.skyline\\.org\\.auth\\.config\\.SecurityConfig"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.skyline\\.org\\.auth\\.config\\.RateLimitFilter")
})
@Import({FlywayMigrationConfig.class, PasswordEncoderConfig.class})
public class OrgAuthCoreAutoConfiguration {
}
