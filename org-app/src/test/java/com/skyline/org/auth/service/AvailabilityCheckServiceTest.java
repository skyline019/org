package com.skyline.org.auth.service;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.dto.AvailabilityResponse;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvailabilityCheckServiceTest {

    @Test
    void reportsUsernameAvailability() {
        UserService userService = mock(UserService.class);
        when(userService.isUsernameAvailable("newuser")).thenReturn(true);
        AuthProperties props = new AuthProperties();
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.check.username.available", Locale.getDefault(), "available");
        Messages messages = new Messages(source);
        AvailabilityCheckService service = new AvailabilityCheckService(userService, props, messages);

        AvailabilityResponse response = service.checkUsername("newuser");
        assertThat(response.available()).isTrue();
    }

    @Test
    void enumerationSafeModeHidesTakenStatus() {
        UserService userService = mock(UserService.class);
        AuthProperties props = new AuthProperties();
        props.getAuth().getCheck().setEnumerationSafe(true);
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.check.username.format-valid", Locale.getDefault(), "ok");
        Messages messages = new Messages(source);
        AvailabilityCheckService service = new AvailabilityCheckService(userService, props, messages);

        AvailabilityResponse response = service.checkUsername("anyone");
        assertThat(response.available()).isTrue();
    }
}
