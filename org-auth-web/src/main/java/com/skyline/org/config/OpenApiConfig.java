package com.skyline.org.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Org Auth API")
                        .description("认证模块 REST API（v1）。表单页面使用 Session + CSRF；JSON 接口需携带 X-XSRF-TOKEN 头。")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("csrf"))
                .components(new Components()
                        .addSecuritySchemes("csrf", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-XSRF-TOKEN")
                                .description("与 Cookie 中的 XSRF-TOKEN 一致")));
    }
}
