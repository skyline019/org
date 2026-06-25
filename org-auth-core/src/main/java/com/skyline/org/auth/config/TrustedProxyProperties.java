package com.skyline.org.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.trusted-proxy")
public class TrustedProxyProperties {

    private boolean enabled = false;
    private List<String> trustedNetworks = defaultNetworks();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTrustedNetworks() {
        return trustedNetworks;
    }

    public void setTrustedNetworks(List<String> trustedNetworks) {
        this.trustedNetworks = trustedNetworks;
    }

    private static List<String> defaultNetworks() {
        List<String> networks = new ArrayList<>();
        networks.add("127.0.0.1");
        networks.add("::1");
        networks.add("10.");
        networks.add("172.16.");
        networks.add("192.168.");
        return networks;
    }
}
