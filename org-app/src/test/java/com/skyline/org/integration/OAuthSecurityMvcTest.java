package com.skyline.org.integration;

import com.skyline.org.auth.security.OrgOAuth2UserService;
import com.skyline.org.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "oauth2"})
@AutoConfigureMockMvc
class OAuthSecurityMvcTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrgOAuth2UserService oauth2UserService;

    @Test
    void oauth2UserServiceIsRegistered() {
        assertThat(oauth2UserService).isNotNull();
    }

    @Test
    void loginPageShowsOAuthProviderLink() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/oauth2/authorization/github")));
    }

    @Test
    void oauthAuthorizationInitiatesProviderRedirect() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github").with(remoteAddr("203.0.113.20")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("github.com")));
    }

    private static RequestPostProcessor remoteAddr(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
