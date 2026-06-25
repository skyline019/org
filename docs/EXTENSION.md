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

2. 实现 `OrgAuthSecurityCustomizer`，在内置 `permitAll` 规则之后、`anyRequest().authenticated()` **之前**追加业务 URL 授权规则：

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

> **数据库**：`demo-business` 默认 `spring.flyway.enabled=false`，开发时需先对目标库执行 Flyway（例如先启动 `org-app` 完成迁移），或自行启用 Flyway。

## 切换为 JWT API（思路）

1. 保留 User 域与注册/验证流程（`org-auth-core`）。
2. 新增 `JwtAuthenticationFilter` 与无 Session 的 Security 链（`STATELESS`）。
3. 表单登录与 JWT 可并存：不同 `SecurityFilterChain` `@Order`。

## 启用 OAuth2 社交登录

OAuth2 客户端与 `oauth2Login()` 已内置在 `org-auth-web`（`SecurityConfig` + `OrgOAuth2UserService`）。启用步骤：

1. 设置 `app.auth.oauth2.enabled=true` 并配置 IdP 客户端（见 `application-oauth2.properties.example`）。
2. 确保 IdP 返回 **已验证邮箱**；`OAuthAccountService` 仅关联 verified email，拒绝 `@oauth.local` 合成邮箱。
3. 生产环境在 IdP 控制台配置回调 URL：`{APP_BASE_URL}/login/oauth2/code/{registrationId}`。

## 启用 Redis 分布式限流

生产 profile 已默认启用 Redis 限流。本地或 staging 可显式激活：

```properties
spring.profiles.active=dev,redis
app.auth.rate-limit.backend=redis
spring.data.redis.host=${REDIS_HOST}
```

`application-redis.properties` 会取消 `application.properties` 中的 Redis autoconfigure exclude。

> **语义**：`memory` 与 `redis` 后端均使用 **令牌桶**（Redis 通过 Lua 脚本实现），多实例部署时以 Redis 为准。

## 启用 MFA（TOTP）

1. 复制 `application-mfa.properties.example` 或在 prod 中确认 `app.auth.mfa.enabled=true`。
2. 生产环境设置 `MFA_SECRET_ENCRYPTION_KEY`（`openssl rand -base64 32`）；dev 可用 `app.auth.mfa.secret-encryption.mode=none`。
3. 强制角色（如管理员）配置 `app.auth.mfa.enforce-for-roles=ROLE_ADMIN`。
4. 用户自助注册：`/auth/mfa/setup`；登录后 step-up：`/auth/mfa/challenge`（支持恢复码）。

详见 [SECURITY.md](SECURITY.md) 与 [ARCHITECTURE.md](ARCHITECTURE.md) §3.4。

## 国际化

- 文案键：`org-auth-web/src/main/resources/messages*.properties`（`auth.*` 业务消息、`page.*` 页面模板）
- 代码中统一使用 `Messages.get("auth.xxx")` 或 `Messages.get("page.xxx")`
- Thymeleaf 模板使用 `th:text="#{page.xxx}"`；前端 JS 通过 layout 注入 `window.authI18n`
- 默认 locale 由 Spring `Accept-Language` 或 `spring.web.locale` 决定
