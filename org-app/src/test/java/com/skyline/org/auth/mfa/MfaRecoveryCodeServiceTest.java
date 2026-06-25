package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.UserMfaRecoveryCode;
import com.skyline.org.auth.repository.UserMfaRecoveryCodeRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaRecoveryCodeServiceTest {

    @Mock UserMfaRecoveryCodeRepository recoveryCodeRepository;
    @Mock UserService userService;

    AuthProperties authProperties;
    MfaRecoveryCodeService service;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.getAuth().getMfa().setRecoveryCodeCount(3);
        service = new MfaRecoveryCodeService(
                recoveryCodeRepository,
                userService,
                new BCryptPasswordEncoder(),
                authProperties);
    }

    @Test
    void looksLikeRecoveryCodeDistinguishesTotpFromRecovery() {
        assertThat(MfaRecoveryCodeService.looksLikeRecoveryCode("123456")).isFalse();
        assertThat(MfaRecoveryCodeService.looksLikeRecoveryCode("ABCD-EFGH")).isTrue();
    }

    @Test
    void regenerateForUserStoresHashedCodes() {
        User user = new User();
        user.setId(9L);
        when(recoveryCodeRepository.save(any(UserMfaRecoveryCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> codes = service.regenerateForUser(user);

        assertThat(codes).hasSize(3);
        assertThat(codes.get(0)).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
        verify(recoveryCodeRepository).deleteByUserId(9L);
        ArgumentCaptor<UserMfaRecoveryCode> captor = ArgumentCaptor.forClass(UserMfaRecoveryCode.class);
        verify(recoveryCodeRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getValue().getCodeHash()).startsWith("$2");
    }

    @Test
    void tryConsumeMarksMatchingCodeAsUsed() {
        User user = new User();
        user.setId(3L);
        user.setUsername("alice");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UserMfaRecoveryCode stored = new UserMfaRecoveryCode(user, encoder.encode("ABCD1234"));
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(recoveryCodeRepository.findUnusedByUserId(3L)).thenReturn(List.of(stored));
        when(recoveryCodeRepository.save(stored)).thenReturn(stored);

        assertThat(service.tryConsume("alice", "ABCD-1234")).isTrue();
        assertThat(stored.getUsedAt()).isNotNull();
    }
}
