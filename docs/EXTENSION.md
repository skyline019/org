# Org Auth — 扩展指南

## 嵌入业务系统（推荐）

1. 在业务 `pom.xml` 中引入 `org-auth-starter`：

```xml
<dependency>
    <groupId>com.skyline</groupId>
    <artifactId>org-auth-starter</artifactId>
    <version>${org.version}</version>
</dependency>
```

2. 实现 `OrgAuthSecurityCustomizer` 为业务 URL 追加授权规则（在 auth 默认规则之后执行）：

```java
@Bean
OrgAuthSecurityCustomizer businessUrls() {
    return auth -> auth.requestMatchers("/dashboard/**").authenticated();
}
```

3. 配置登录成功跳转（可选）：

```properties
app.auth.login-success-url=/dashboard
```

4. 参考 `demo-business` 模块：`DemoBusinessApplication`、`DashboardController`、`DemoSecurityCustomizer`。

## 切换为 JWT API（思路）

1. 保留 User 域与注册/验证流程（`org-auth-core`）。
2. 新增 `JwtAuthenticationFilter` 与无 Session 的 Security 链（`STATELESS`）。
3. 表单登录与 JWT 可并存：不同 `SecurityFilterChain` `@Order`。

## 启用 OAuth2 社交登录

1. 添加 `spring-boot-starter-oauth2-client`。
2. 在 SecurityConfig 增加 `oauth2Login()`。
3. 关联 OAuth 用户与本地 User（按 email 绑定或独立表）。

## 启用 Redis 分布式限流

```properties
spring.profiles.active=prod,redis
app.auth.rate-limit.backend=redis
spring.data.redis.host=${REDIS_HOST}
```

取消 `application.properties` 中 Redis autoconfigure exclude（`application-redis.properties` 已示例）。

> **语义差异**：`memory` 后端使用 Bucket4j 令牌桶（平滑 refill）；`redis` 后端使用按分钟固定窗口计数。两者在 burst 行为上略有不同，多实例部署时以 Redis 为准。

## 国际化

- 文案键：`org-auth-web/src/main/resources/messages*.properties`（`auth.*` 业务消息、`page.*` 页面模板）
- 代码中统一使用 `Messages.get("auth.xxx")` 或 `Messages.get("page.xxx")`
- Thymeleaf 模板使用 `th:text="#{page.xxx}"`；前端 JS 通过 layout 注入 `window.authI18n`
- 默认 locale 由 Spring `Accept-Language` 或 `spring.web.locale` 决定
