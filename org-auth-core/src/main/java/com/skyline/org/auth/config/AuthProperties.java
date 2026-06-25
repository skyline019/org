package com.skyline.org.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app")
public class AuthProperties {

    private String baseUrl = "http://localhost:8080";
    private final Auth auth = new Auth();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Auth getAuth() {
        return auth;
    }

    public static class Auth {
        private String loginSuccessUrl = "/home";
        private final Lock lock = new Lock();
        private final Token token = new Token();
        private final RateLimit rateLimit = new RateLimit();
        private final Maintenance maintenance = new Maintenance();
        private final Check check = new Check();
        private final Audit audit = new Audit();
        private final OAuth2 oauth2 = new OAuth2();
        private final BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
        private final Mfa mfa = new Mfa();

        public OAuth2 getOauth2() {
            return oauth2;
        }

        public BootstrapAdmin getBootstrapAdmin() {
            return bootstrapAdmin;
        }

        public Lock getLock() {
            return lock;
        }

        public Token getToken() {
            return token;
        }

        public RateLimit getRateLimit() {
            return rateLimit;
        }

        public Maintenance getMaintenance() {
            return maintenance;
        }

        public Check getCheck() {
            return check;
        }

        public Audit getAudit() {
            return audit;
        }

        public String getLoginSuccessUrl() {
            return loginSuccessUrl;
        }

        public void setLoginSuccessUrl(String loginSuccessUrl) {
            this.loginSuccessUrl = loginSuccessUrl;
        }

        public Mfa getMfa() {
            return mfa;
        }
    }

    public static class Lock {
        private int maxAttempts = 5;
        private Duration duration = Duration.ofMinutes(15);

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getDuration() {
            return duration;
        }

        public void setDuration(Duration duration) {
            this.duration = duration;
        }
    }

    public static class Token {
        private Duration emailVerificationExpiry = Duration.ofHours(24);
        private Duration passwordResetExpiry = Duration.ofMinutes(30);

        public Duration getEmailVerificationExpiry() {
            return emailVerificationExpiry;
        }

        public void setEmailVerificationExpiry(Duration emailVerificationExpiry) {
            this.emailVerificationExpiry = emailVerificationExpiry;
        }

        public Duration getPasswordResetExpiry() {
            return passwordResetExpiry;
        }

        public void setPasswordResetExpiry(Duration passwordResetExpiry) {
            this.passwordResetExpiry = passwordResetExpiry;
        }
    }

    public static class RateLimit {
        private String backend = "memory";
        private int loginPerMinute = 10;
        private int registerPerMinute = 5;
        private int resetPerMinute = 5;
        private int checkPerMinute = 30;
        private int resendPerMinute = 3;
        private int adminPerMinute = 30;

        public String getBackend() {
            return backend;
        }

        public void setBackend(String backend) {
            this.backend = backend;
        }

        public int getLoginPerMinute() {
            return loginPerMinute;
        }

        public void setLoginPerMinute(int loginPerMinute) {
            this.loginPerMinute = loginPerMinute;
        }

        public int getRegisterPerMinute() {
            return registerPerMinute;
        }

        public void setRegisterPerMinute(int registerPerMinute) {
            this.registerPerMinute = registerPerMinute;
        }

        public int getResetPerMinute() {
            return resetPerMinute;
        }

        public void setResetPerMinute(int resetPerMinute) {
            this.resetPerMinute = resetPerMinute;
        }

        public int getCheckPerMinute() {
            return checkPerMinute;
        }

        public void setCheckPerMinute(int checkPerMinute) {
            this.checkPerMinute = checkPerMinute;
        }

        public int getResendPerMinute() {
            return resendPerMinute;
        }

        public void setResendPerMinute(int resendPerMinute) {
            this.resendPerMinute = resendPerMinute;
        }

        public int getAdminPerMinute() {
            return adminPerMinute;
        }

        public void setAdminPerMinute(int adminPerMinute) {
            this.adminPerMinute = adminPerMinute;
        }
    }

    public static class Check {
        private boolean enumerationSafe = false;
        private Duration availabilityCacheTtl = Duration.ofMinutes(5);

        public boolean isEnumerationSafe() {
            return enumerationSafe;
        }

        public void setEnumerationSafe(boolean enumerationSafe) {
            this.enumerationSafe = enumerationSafe;
        }

        public Duration getAvailabilityCacheTtl() {
            return availabilityCacheTtl;
        }

        public void setAvailabilityCacheTtl(Duration availabilityCacheTtl) {
            this.availabilityCacheTtl = availabilityCacheTtl;
        }
    }

    public static class Audit {
        private boolean persist = false;
        private Duration retention = Duration.ofDays(90);

        public boolean isPersist() {
            return persist;
        }

        public void setPersist(boolean persist) {
            this.persist = persist;
        }

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }
    }

    public static class OAuth2 {
        private boolean enabled = false;
        private java.util.List<String> providers = new java.util.ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public java.util.List<String> getProviders() {
            return providers;
        }

        public void setProviders(java.util.List<String> providers) {
            this.providers = providers;
        }
    }

    public static class BootstrapAdmin {
        private boolean enabled = false;
        private String username = "admin";
        private String email = "admin@example.com";
        private String password;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Mfa {
        private boolean enabled = false;
        private String issuer = "Org Auth";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Maintenance {
        private Duration loginAttemptRetention = Duration.ofDays(30);
        private Duration expiredTokenRetention = Duration.ofDays(7);

        public Duration getLoginAttemptRetention() {
            return loginAttemptRetention;
        }

        public void setLoginAttemptRetention(Duration loginAttemptRetention) {
            this.loginAttemptRetention = loginAttemptRetention;
        }

        public Duration getExpiredTokenRetention() {
            return expiredTokenRetention;
        }

        public void setExpiredTokenRetention(Duration expiredTokenRetention) {
            this.expiredTokenRetention = expiredTokenRetention;
        }
    }
}
