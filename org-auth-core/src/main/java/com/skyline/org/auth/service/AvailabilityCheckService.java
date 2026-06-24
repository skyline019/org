package com.skyline.org.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.dto.AvailabilityResponse;
import com.skyline.org.auth.validation.EmailValidator;
import com.skyline.org.auth.validation.UsernameValidator;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AvailabilityCheckService {

    private final UserService userService;
    private final AuthProperties authProperties;
    private final Messages messages;
    private final Cache<String, Boolean> usernameCache;
    private final Cache<String, Boolean> emailCache;

    public AvailabilityCheckService(
            UserService userService,
            AuthProperties authProperties,
            Messages messages) {
        this.userService = userService;
        this.authProperties = authProperties;
        this.messages = messages;
        long ttlSeconds = authProperties.getAuth().getCheck().getAvailabilityCacheTtl().toSeconds();
        this.usernameCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
        this.emailCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }

    public AvailabilityResponse checkUsername(String value) {
        return UsernameValidator.validateFormat(value)
                .map(key -> new AvailabilityResponse(false, messages.get(key)))
                .orElseGet(() -> buildResponse(
                        value,
                        usernameCache,
                        userService::isUsernameAvailable,
                        "auth.check.username.available",
                        "auth.check.username.taken",
                        "auth.check.username.format-valid"));
    }

    public AvailabilityResponse checkEmail(String value) {
        return EmailValidator.validateFormat(value)
                .map(key -> new AvailabilityResponse(false, messages.get(key)))
                .orElseGet(() -> buildResponse(
                        value,
                        emailCache,
                        userService::isEmailAvailable,
                        "auth.check.email.available",
                        "auth.check.email.taken",
                        "auth.check.email.format-valid"));
    }

    private AvailabilityResponse buildResponse(
            String value,
            Cache<String, Boolean> cache,
            java.util.function.Function<String, Boolean> availabilityFn,
            String availableKey,
            String takenKey,
            String formatValidKey) {
        if (authProperties.getAuth().getCheck().isEnumerationSafe()) {
            return new AvailabilityResponse(true, messages.get(formatValidKey));
        }
        boolean available = cache.get(value, availabilityFn);
        String messageKey = available ? availableKey : takenKey;
        return new AvailabilityResponse(available, messages.get(messageKey));
    }
}
