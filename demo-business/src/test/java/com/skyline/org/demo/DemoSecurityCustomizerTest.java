package com.skyline.org.demo;

import com.skyline.org.auth.security.OrgAuthSecurityCustomizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoSecurityCustomizerTest {

    @Test
    void exposesOrgAuthSecurityCustomizerBean() {
        DemoSecurityCustomizer config = new DemoSecurityCustomizer();

        OrgAuthSecurityCustomizer customizer = config.dashboardSecurity();

        assertThat(customizer).isNotNull();
        assertThat(customizer).isInstanceOf(OrgAuthSecurityCustomizer.class);
    }
}
