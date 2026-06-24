package com.skyline.org.auth.event;

public record VerificationEmailRequested(String email, String verificationUrl, String expiryDescription) {
}
