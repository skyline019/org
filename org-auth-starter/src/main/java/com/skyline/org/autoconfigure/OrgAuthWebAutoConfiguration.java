package com.skyline.org.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration(after = OrgAuthCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ComponentScan(basePackages = {
        "com.skyline.org.auth.controller",
        "com.skyline.org.auth.security",
        "com.skyline.org.auth.config",
        "com.skyline.org.config",
        "com.skyline.org.common.exception"
})
public class OrgAuthWebAutoConfiguration {
}
