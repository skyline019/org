package com.skyline.org.auth.service;

import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.auth.repository.LoginAttemptRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAttemptServiceTest {

    @Test
    void resetsLockOnSuccess() {
        UserService userService = mock(UserService.class);
        LoginAttemptRepository repo = mock(LoginAttemptRepository.class);
        AccountLockService lockService = mock(AccountLockService.class);
        User user = new User();
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        LoginAttemptService service = new LoginAttemptService(userService, repo, lockService);

        service.recordSuccess("alice", "127.0.0.1");
        verify(lockService).resetLockState(user);
    }
}
