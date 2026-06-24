package com.skyline.org.auth.security;

import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;
    private final AccountLockService accountLockService;
    private final Messages messages;

    public CustomUserDetailsService(
            UserService userService,
            AccountLockService accountLockService,
            Messages messages) {
        this.userService = userService;
        this.accountLockService = accountLockService;
        this.messages = messages;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findByUsernameForLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException(messages.get("auth.login.error")));
        accountLockService.unlockIfExpired(user);
        return new CustomUserDetails(user);
    }
}
