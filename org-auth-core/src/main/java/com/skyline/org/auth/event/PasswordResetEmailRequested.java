package com.skyline.org.auth.event;

public record PasswordResetEmailRequested(String email, String resetUrl, String expiryDescription) {
}
