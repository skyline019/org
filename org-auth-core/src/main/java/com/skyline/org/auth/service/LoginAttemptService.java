package com.skyline.org.auth.service;

import com.skyline.org.auth.entity.LoginAttempt;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.repository.LoginAttemptRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {

    private final UserService userService;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AccountLockService accountLockService;

    public LoginAttemptService(
            UserService userService,
            LoginAttemptRepository loginAttemptRepository,
            AccountLockService accountLockService) {
        this.userService = userService;
        this.loginAttemptRepository = loginAttemptRepository;
        this.accountLockService = accountLockService;
    }

    @Transactional
    public void recordSuccess(String username, String ipAddress) {
        loginAttemptRepository.save(new LoginAttempt(username, ipAddress, true));
        userService.findByUsername(username).ifPresent(accountLockService::resetLockState);
    }

    @Transactional
    public void recordFailure(String username, String ipAddress) {
        loginAttemptRepository.save(new LoginAttempt(username, ipAddress, false));
        userService.findByUsername(username).ifPresent(accountLockService::recordFailedAttempt);
    }
}
