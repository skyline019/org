package com.skyline.org.common.web;

import com.skyline.org.auth.config.TrustedProxyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void ignoresForwardedHeadersWhenTrustedProxyDisabled() {
        TrustedProxyProperties props = new TrustedProxyProperties();
        props.setEnabled(false);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        request.setRemoteAddr("198.51.100.9");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void resolvesForwardedClientIpFromTrustedProxy() {
        TrustedProxyProperties props = new TrustedProxyProperties();
        props.setEnabled(true);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void fallsBackToRemoteAddrWhenProxyNotTrusted() {
        TrustedProxyProperties props = new TrustedProxyProperties();
        props.setEnabled(true);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        request.setRemoteAddr("198.51.100.9");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void usesXRealIpFromTrustedProxy() {
        TrustedProxyProperties props = new TrustedProxyProperties();
        props.setEnabled(true);
        ClientIpResolver resolver = new ClientIpResolver(props);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.5");
        request.setRemoteAddr("10.0.0.5");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.5");
    }
}
