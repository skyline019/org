package com.skyline.org.auth.oauth;

import com.skyline.org.auth.entity.OAuthAccount;
import com.skyline.org.auth.repository.OAuthAccountRepository;
import com.skyline.org.common.util.TokenGenerator;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "app.auth.oauth2.enabled", havingValue = "true")
public class OAuthAccountService {

    private static final Pattern USERNAME_SANITIZER = Pattern.compile("[^a-zA-Z0-9_]");
    private static final String UNUSABLE_PASSWORD = TokenGenerator.generateRawToken();

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    public OAuthAccountService(
            OAuthAccountRepository oauthAccountRepository,
            UserService userService,
            PasswordEncoder passwordEncoder) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OAuth2User resolveOAuthUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerUserId = resolveProviderUserId(oauth2User);
        User user = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(OAuthAccount::getUser)
                .orElseGet(() -> linkOrCreateUser(provider, providerUserId, oauth2User));
        if (!user.isEnabled()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "account_disabled",
                    "Account is disabled",
                    null));
        }
        return toOAuth2Principal(oauth2User, user);
    }

    private User linkOrCreateUser(String provider, String providerUserId, OAuth2User oauth2User) {
        User user = resolveLocalUser(oauth2User);
        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider(provider);
        account.setProviderUserId(providerUserId);
        oauthAccountRepository.save(account);
        return user;
    }

    private User resolveLocalUser(OAuth2User oauth2User) {
        String email = requireProviderEmail(oauth2User);
        User existing = userService.findByEmail(email);
        if (existing != null) {
            if (!existing.isEmailVerified()) {
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        "email_not_verified",
                        "Local account exists but email is not verified; verify email before using OAuth",
                        null));
            }
            return existing;
        }
        return createOAuthUser(oauth2User, email);
    }

    private User createOAuthUser(OAuth2User oauth2User, String email) {
        String username = resolveUsername(oauth2User, email);
        String passwordHash = passwordEncoder.encode(UNUSABLE_PASSWORD);
        User user = userService.createOAuthUser(username, email, passwordHash);
        user.setEnabled(true);
        user.setEmailVerified(true);
        userService.save(user);
        return user;
    }

    private static String requireProviderEmail(OAuth2User oauth2User) {
        String email = stringAttribute(oauth2User, "email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "missing_email",
                    "OAuth provider did not return an email address",
                    null));
        }
        return email.trim();
    }

    private String resolveUsername(OAuth2User oauth2User, String email) {
        String login = stringAttribute(oauth2User, "login");
        if (login != null && !login.isBlank()) {
            return ensureUniqueUsername(sanitize(login));
        }
        String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        return ensureUniqueUsername(sanitize(localPart));
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;
        while (!userService.isUsernameAvailable(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String sanitize(String value) {
        String sanitized = USERNAME_SANITIZER.matcher(value).replaceAll("_");
        if (sanitized.length() < 3) {
            sanitized = sanitized + "_oauth";
        }
        return sanitized.substring(0, Math.min(sanitized.length(), 50));
    }

    private static String resolveProviderUserId(OAuth2User oauth2User) {
        String sub = stringAttribute(oauth2User, "sub");
        if (sub != null && !sub.isBlank()) {
            return sub;
        }
        String id = stringAttribute(oauth2User, "id");
        if (id != null && !id.isBlank()) {
            return id;
        }
        return oauth2User.getName();
    }

    private static String stringAttribute(OAuth2User oauth2User, String key) {
        Object value = oauth2User.getAttribute(key);
        return value == null ? null : value.toString();
    }

    private static OAuth2User toOAuth2Principal(OAuth2User source, User user) {
        Map<String, Object> attributes = new HashMap<>(source.getAttributes());
        attributes.put("username", user.getUsername());
        return new DefaultOAuth2User(
                user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                        .toList(),
                attributes,
                "username");
    }
}
