package com.skyline.org.auth.schedule;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.repository.EmailVerificationTokenRepository;
import com.skyline.org.auth.repository.LoginAttemptRepository;
import com.skyline.org.auth.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthMaintenanceSchedulerTest {

    @Test
    void purgesStaleRecords() {
        LoginAttemptRepository loginRepo = mock(LoginAttemptRepository.class);
        EmailVerificationTokenRepository emailRepo = mock(EmailVerificationTokenRepository.class);
        PasswordResetTokenRepository resetRepo = mock(PasswordResetTokenRepository.class);
        when(loginRepo.deleteByAttemptedAtBefore(any())).thenReturn(1);
        when(emailRepo.deleteByExpiresAtBefore(any())).thenReturn(2);
        when(resetRepo.deleteByExpiresAtBefore(any())).thenReturn(3);

        AuthProperties props = new AuthProperties();
        props.getAuth().getMaintenance().setLoginAttemptRetention(Duration.ofDays(30));
        props.getAuth().getMaintenance().setExpiredTokenRetention(Duration.ofDays(7));

        AuthMaintenanceScheduler scheduler = new AuthMaintenanceScheduler(loginRepo, emailRepo, resetRepo, props);
        scheduler.purgeStaleData();

        verify(loginRepo).deleteByAttemptedAtBefore(any());
        verify(emailRepo).deleteByExpiresAtBefore(any());
        verify(resetRepo).deleteByExpiresAtBefore(any());
    }
}
