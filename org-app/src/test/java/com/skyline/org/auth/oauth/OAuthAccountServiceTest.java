package com.skyline.org.auth.oauth;

import com.skyline.org.auth.entity.OAuthAccount;
import com.skyline.org.auth.repository.OAuthAccountRepository;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock OAuthAccountRepository oauthAccountRepository;
    @Mock UserService userService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock DefaultOAuth2UserService delegate;

    OAuthAccountService service;

    @BeforeEach
    void setUp() {
        service = new OAuthAccountService(oauthAccountRepository, userService, passwordEncoder);
        ReflectionTestUtils.setField(service, "delegate", delegate);
    }

    @Test
    void rejectsLinkingUnverifiedLocalAccount() {
        OAuth2User providerUser = providerUser("victim@example.com", "sub-1");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-1"))
                .thenReturn(Optional.empty());

        User unverified = localUser("victim", "victim@example.com", false, false);
        when(userService.findByEmail("victim@example.com")).thenReturn(unverified);

        assertThatThrownBy(() -> service.resolveOAuthUser(userRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(oauthAccountRepository, never()).save(any());
    }

    @Test
    void linksVerifiedLocalAccountWithoutChangingEnabledState() {
        OAuth2User providerUser = providerUser("verified@example.com", "sub-2");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-2"))
                .thenReturn(Optional.empty());

        User verified = localUser("verified", "verified@example.com", true, true);
        when(userService.findByEmail("verified@example.com")).thenReturn(verified);

        OAuth2User principal = service.resolveOAuthUser(userRequest());

        assertThat(principal.getName()).isEqualTo("verified");
        verify(userService, never()).save(verified);
        verify(oauthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void rejectsDisabledVerifiedAccount() {
        OAuth2User providerUser = providerUser("disabled@example.com", "sub-disabled");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-disabled"))
                .thenReturn(Optional.empty());

        User disabled = localUser("disabled", "disabled@example.com", false, true);
        when(userService.findByEmail("disabled@example.com")).thenReturn(disabled);

        assertThatThrownBy(() -> service.resolveOAuthUser(userRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsProviderWithoutEmail() {
        OAuth2User providerUser = new DefaultOAuth2User(
                List.of(), Map.of("sub", "sub-3"), "sub");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-3"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveOAuthUser(userRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void createsNewUserWhenEmailNotRegistered() {
        OAuth2User providerUser = providerUser("new@example.com", "sub-4");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-4"))
                .thenReturn(Optional.empty());
        when(userService.findByEmail("new@example.com")).thenReturn(null);
        when(userService.isUsernameAvailable(anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        User created = localUser("new", "new@example.com", true, true);
        when(userService.createOAuthUser(anyString(), anyString(), anyString())).thenReturn(created);

        OAuth2User principal = service.resolveOAuthUser(userRequest());

        assertThat(principal.getName()).isEqualTo("new");
        verify(userService).save(created);
        verify(oauthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void rejectsDisabledExistingOAuthAccount() {
        OAuth2User providerUser = providerUser("linked@example.com", "sub-5");
        when(delegate.loadUser(any())).thenReturn(providerUser);

        User disabled = localUser("linked", "linked@example.com", false, true);
        OAuthAccount existing = new OAuthAccount();
        existing.setUser(disabled);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-5"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.resolveOAuthUser(userRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void returnsPrincipalForExistingLinkedAccount() {
        OAuth2User providerUser = providerUser("linked@example.com", "sub-linked");
        when(delegate.loadUser(any())).thenReturn(providerUser);

        User linked = localUser("linked", "linked@example.com", true, true);
        OAuthAccount existing = new OAuthAccount();
        existing.setUser(linked);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-linked"))
                .thenReturn(Optional.of(existing));

        OAuth2User principal = service.resolveOAuthUser(userRequest());

        assertThat(principal.getName()).isEqualTo("linked");
        verify(oauthAccountRepository, never()).save(any());
    }

    @Test
    void createsUsernameFromProviderLoginAttribute() {
        OAuth2User providerUser = new DefaultOAuth2User(
                List.of(),
                Map.of("email", "gh@example.com", "sub", "sub-gh", "login", "octocat"),
                "sub");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-gh"))
                .thenReturn(Optional.empty());
        when(userService.findByEmail("gh@example.com")).thenReturn(null);
        when(userService.isUsernameAvailable("octocat")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        User created = localUser("octocat", "gh@example.com", true, true);
        when(userService.createOAuthUser("octocat", "gh@example.com", "hash")).thenReturn(created);

        OAuth2User principal = service.resolveOAuthUser(userRequest());

        assertThat(principal.getName()).isEqualTo("octocat");
        verify(userService).createOAuthUser("octocat", "gh@example.com", "hash");
    }

    @Test
    void appendsSuffixWhenGeneratedUsernameIsTaken() {
        OAuth2User providerUser = providerUser("fresh@example.com", "sub-6");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "sub-6"))
                .thenReturn(Optional.empty());
        when(userService.findByEmail("fresh@example.com")).thenReturn(null);
        when(userService.isUsernameAvailable("fresh")).thenReturn(false);
        when(userService.isUsernameAvailable("fresh1")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        User created = localUser("fresh1", "fresh@example.com", true, true);
        when(userService.createOAuthUser("fresh1", "fresh@example.com", "hash")).thenReturn(created);

        OAuth2User principal = service.resolveOAuthUser(userRequest());

        assertThat(principal.getName()).isEqualTo("fresh1");
    }

    @Test
    void resolvesProviderUserIdFromIdAttributeWhenSubMissing() {
        OAuth2User providerUser = new DefaultOAuth2User(
                List.of(),
                Map.of("email", "id@example.com", "id", "provider-id-99"),
                "id");
        when(delegate.loadUser(any())).thenReturn(providerUser);
        when(oauthAccountRepository.findByProviderAndProviderUserId("github", "provider-id-99"))
                .thenReturn(Optional.empty());
        when(userService.findByEmail("id@example.com")).thenReturn(null);
        when(userService.isUsernameAvailable(anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        User created = localUser("id", "id@example.com", true, true);
        when(userService.createOAuthUser(anyString(), anyString(), anyString())).thenReturn(created);

        service.resolveOAuthUser(userRequest());

        verify(oauthAccountRepository).save(org.mockito.ArgumentMatchers.argThat(
                account -> "provider-id-99".equals(account.getProviderUserId())));
    }

    private static OAuth2UserRequest userRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("github")
                .clientId("id")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/github")
                .authorizationUri("https://example.com/auth")
                .tokenUri("https://example.com/token")
                .userInfoUri("https://example.com/user")
                .userNameAttributeName("sub")
                .build();
        return new OAuth2UserRequest(
                registration,
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", null, null));
    }

    private static OAuth2User providerUser(String email, String sub) {
        return new DefaultOAuth2User(
                List.of(),
                Map.of("email", email, "sub", sub),
                "sub");
    }

    private static User localUser(String username, String email, boolean enabled, boolean emailVerified) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(enabled);
        user.setEmailVerified(emailVerified);
        Role role = new Role();
        role.setName("ROLE_USER");
        user.setRoles(new java.util.HashSet<>(Set.of(role)));
        return user;
    }
}
