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

        public String getLoginSuccessUrl() {
            return loginSuccessUrl;
        }

        public void setLoginSuccessUrl(String loginSuccessUrl) {
            this.loginSuccessUrl = loginSuccessUrl;
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
