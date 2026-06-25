package com.skyline.org.auth.mfa;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.entity.UserTotpCredential;
import com.skyline.org.auth.repository.UserTotpRepository;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock UserTotpRepository userTotpRepository;
    @Mock UserService userService;
    @Mock TotpMfaService totpMfaService;

    AuthProperties authProperties;
    MfaService mfaService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.getAuth().getMfa().setEnabled(true);
        mfaService = new MfaService(authProperties, userTotpRepository, userService, totpMfaService);
    }

    @Test
    void requiresChallengeWhenFeatureEnabledAndUserEnrolled() {
        UserTotpCredential credential = new UserTotpCredential(new User(), "secret");
        credential.setEnabled(true);
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));

        assertThat(mfaService.requiresChallenge("alice")).isTrue();
    }

    @Test
    void doesNotRequireChallengeWhenFeatureDisabled() {
        authProperties.getAuth().getMfa().setEnabled(false);

        assertThat(mfaService.requiresChallenge("alice")).isFalse();
    }

    @Test
    void doesNotRequireChallengeWhenUserNotEnrolled() {
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThat(mfaService.requiresChallenge("alice")).isFalse();
    }

    @Test
    void doesNotRequireChallengeWhenCredentialDisabled() {
        UserTotpCredential credential = new UserTotpCredential(new User(), "secret");
        credential.setEnabled(false);
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));

        assertThat(mfaService.requiresChallenge("alice")).isFalse();
    }

    @Test
    void isEnrolledReflectsEnabledCredential() {
        UserTotpCredential credential = new UserTotpCredential(new User(), "secret");
        credential.setEnabled(true);
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));
        when(userTotpRepository.findByUsername("bob")).thenReturn(Optional.empty());

        assertThat(mfaService.isEnrolled("alice")).isTrue();
        assertThat(mfaService.isEnrolled("bob")).isFalse();
    }

    @Test
    void verifyChallengeReturnsFalseWhenNotEnrolled() {
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThat(mfaService.verifyChallenge("alice", "123456")).isFalse();
    }

    @Test
    void disableTurnsOffStoredCredential() {
        UserTotpCredential credential = new UserTotpCredential(new User(), "secret");
        credential.setEnabled(true);
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));

        mfaService.disable("alice");

        assertThat(credential.isEnabled()).isFalse();
        verify(userTotpRepository).save(credential);
    }

    @Test
    void beginEnrollmentUpdatesExistingCredential() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        UserTotpCredential existing = new UserTotpCredential(user, "OLDSECRET");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(totpMfaService.generateSecret()).thenReturn("NEWSECRET");
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(userTotpRepository.save(existing)).thenReturn(existing);

        assertThat(mfaService.beginEnrollment("alice")).isEqualTo("NEWSECRET");
        assertThat(existing.getSecret()).isEqualTo("NEWSECRET");
        assertThat(existing.isEnabled()).isFalse();
    }

    @Test
    void confirmEnrollmentVerifiesCodeBeforeEnabling() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        UserTotpCredential credential = new UserTotpCredential(user, "secret");
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));
        when(totpMfaService.verifyCode("secret", "123456")).thenReturn(true);

        mfaService.confirmEnrollment("alice", "123456");

        ArgumentCaptor<UserTotpCredential> captor = ArgumentCaptor.forClass(UserTotpCredential.class);
        verify(userTotpRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    void confirmEnrollmentRejectsInvalidCode() {
        User user = new User();
        user.setUsername("alice");
        UserTotpCredential credential = new UserTotpCredential(user, "secret");
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));
        when(totpMfaService.verifyCode("secret", "000000")).thenReturn(false);

        assertThatThrownBy(() -> mfaService.confirmEnrollment("alice", "000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void beginEnrollmentStoresPendingSecret() {
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(Optional.of(user));
        when(totpMfaService.generateSecret()).thenReturn("NEWSECRET");
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userTotpRepository.save(any(UserTotpCredential.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String secret = mfaService.beginEnrollment("alice");

        assertThat(secret).isEqualTo("NEWSECRET");
        verify(userTotpRepository).save(any(UserTotpCredential.class));
    }

    @Test
    void verifyChallengeUsesStoredSecret() {
        UserTotpCredential credential = new UserTotpCredential(new User(), "secret");
        credential.setEnabled(true);
        when(userTotpRepository.findByUsername("alice")).thenReturn(Optional.of(credential));
        when(totpMfaService.verifyCode("secret", "654321")).thenReturn(true);

        assertThat(mfaService.verifyChallenge("alice", "654321")).isTrue();
    }
}
