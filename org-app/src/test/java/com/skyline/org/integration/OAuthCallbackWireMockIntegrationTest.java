package com.skyline.org.integration;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.entity.UserTotpCredential;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.auth.repository.UserTotpRepository;
import com.skyline.org.auth.security.OrgOAuth2UserService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.testsupport.AbstractIntegrationTest;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "oauth2"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OAuthCallbackWireMockIntegrationTest extends AbstractIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @LocalServerPort
    int port;

    @Autowired MockMvc mockMvc;
    @Autowired ClientRegistrationRepository clientRegistrationRepository;
    @Autowired OrgOAuth2UserService orgOAuth2UserService;
    @Autowired com.skyline.org.auth.repository.OAuthAccountRepository oauthAccountRepository;

    @DynamicPropertySource
    static void wireMockOAuthProvider(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.provider.github.authorization-uri",
                () -> wireMock.baseUrl() + "/login/oauth/authorize");
        registry.add("spring.security.oauth2.client.provider.github.token-uri",
                () -> wireMock.baseUrl() + "/login/oauth/access_token");
        registry.add("spring.security.oauth2.client.provider.github.user-info-uri",
                () -> wireMock.baseUrl() + "/user");
        registry.add("spring.security.oauth2.client.provider.github.user-name-attribute", () -> "id");
    }

    @BeforeEach
    void stubGithubEndpoints() {
        wireMock.resetAll();
        wireMock.stubFor(post(urlEqualTo("/login/oauth/access_token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"access_token":"wiremock-token","token_type":"bearer","scope":"read:user"}
                                """)));

        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/user"))
                .withHeader("Authorization", equalTo("Bearer wiremock-token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"login":"wiremock_user","id":424242,"email":"wiremock@test.local","name":"WireMock User"}
                                """)));
    }

    @Test
    void oauthProviderEndpointsAreWiredToWireMock() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("github");
        assertThat(registration).isNotNull();
        assertThat(registration.getProviderDetails().getTokenUri()).startsWith(wireMock.baseUrl());
        assertThat(registration.getProviderDetails().getUserInfoEndpoint().getUri()).startsWith(wireMock.baseUrl());
    }

    @Test
    void authorizationRedirectTargetsWireMock() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github").with(localPort(port)))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString(wireMock.baseUrl())));
    }

    @Test
    void oauthUserInfoFromWireMockLinksLocalAccount() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("github");
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "wiremock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600));
        OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, accessToken);

        OAuth2User oauthUser = orgOAuth2UserService.loadUser(userRequest);

        assertThat(oauthUser.getName()).isNotBlank();
        assertThat(oauthAccountRepository.findByProviderAndProviderUserId("github", "424242")).isPresent();
    }

    private static RequestPostProcessor localPort(int port) {
        return request -> {
            request.setServerPort(port);
            request.setServerName("localhost");
            request.setScheme("http");
            return request;
        };
    }
}
