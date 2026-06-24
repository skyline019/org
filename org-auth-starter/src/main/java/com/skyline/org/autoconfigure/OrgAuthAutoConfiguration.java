package com.skyline.org.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({OrgAuthCoreAutoConfiguration.class, OrgAuthWebAutoConfiguration.class})
public class OrgAuthAutoConfiguration {
}
