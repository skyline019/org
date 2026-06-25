package com.skyline.org.auth.config;

import com.skyline.org.auth.security.CustomUserDetailsService;
import com.skyline.org.auth.security.LoginFailureHandler;
import com.skyline.org.auth.security.LoginSuccessHandler;
import com.skyline.org.auth.security.OrgAuthSecurityCustomizer;
import com.skyline.org.auth.security.OrgOAuth2UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final RateLimitFilter rateLimitFilter;
    private final ObjectProvider<List<OrgAuthSecurityCustomizer>> securityCustomizers;
    private final ObjectProvider<OrgOAuth2UserService> oauth2UserService;
    private final Environment environment;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            RateLimitFilter rateLimitFilter,
            ObjectProvider<List<OrgAuthSecurityCustomizer>> securityCustomizers,
            ObjectProvider<OrgOAuth2UserService> oauth2UserService,
            Environment environment) {
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.rateLimitFilter = rateLimitFilter;
        this.securityCustomizers = securityCustomizers;
        this.oauth2UserService = oauth2UserService;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/", "/login", "/register", "/auth/**",
                            "/oauth2/**", "/login/oauth2/**",
                            "/api/v1/auth/check/**",
                            "/api/v1/auth/register",
                            "/api/v1/auth/resend-verification",
                            "/css/**", "/js/**", "/error",
                            "/actuator/health", "/actuator/info", "/actuator/prometheus"
                    ).permitAll();
                    if (!isProdProfile()) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    } else {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN");
                    }
                    auth.requestMatchers("/admin/**").hasRole("ADMIN")
                            .requestMatchers("/home").authenticated();
                    securityCustomizers.ifAvailable(customizers -> customizers.forEach(customizer -> {
                        try {
                            customizer.customize(auth);
                        } catch (Exception ex) {
                            throw new IllegalStateException("OrgAuthSecurityCustomizer failed", ex);
                        }
                    }));
                    auth.anyRequest().authenticated();
                })
                .userDetailsService(userDetailsService);
        oauth2UserService.ifAvailable(service -> {
            try {
                http.oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(service))
                        .successHandler(loginSuccessHandler));
            } catch (Exception ex) {
                throw new IllegalStateException("OAuth2 login configuration failed", ex);
            }
        });
        http.formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("SESSION")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(3)
                        .maxSessionsPreventsLogin(false)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/v1/auth/check/**")
                )
                .headers(headers -> {
                    headers
                            .contentSecurityPolicy(csp -> csp.policyDirectives(
                                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; connect-src 'self'; img-src 'self' data:"
                            ))
                            .frameOptions(frame -> frame.deny())
                            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
                    if (isProdProfile()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)
                                .preload(false));
                    }
                })
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
