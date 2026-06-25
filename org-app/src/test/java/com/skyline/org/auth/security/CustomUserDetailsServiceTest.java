package com.skyline.org.auth.security;

import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock UserService userService;
    @Mock AccountLockService accountLockService;

    CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.login.error", Locale.getDefault(), "Invalid username or password");
        userDetailsService = new CustomUserDetailsService(
                userService, accountLockService, new Messages(source));
    }

    @Test
    void loadsUserAndUnlocksIfExpired() {
        User user = activeUser("alice");
        when(userService.findByUsernameForLogin("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        verify(accountLockService).unlockIfExpired(user);
        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }

    @Test
    void throwsWhenUserMissing() {
        when(userService.findByUsernameForLogin("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private static User activeUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hash");
        user.setEnabled(true);
        user.setEmailVerified(true);
        Role role = new Role();
        role.setName("ROLE_USER");
        user.setRoles(Set.of(role));
        return user;
    }
}
