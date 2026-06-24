package com.skyline.org.auth.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Extension point for embedding applications to add URL authorization rules
 * before the default {@code anyRequest().authenticated()} rule.
 */
@FunctionalInterface
public interface OrgAuthSecurityCustomizer {

    void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth)
            throws Exception;
}
