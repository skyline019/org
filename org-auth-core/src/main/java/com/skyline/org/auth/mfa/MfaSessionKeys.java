package com.skyline.org.auth.mfa;

/**
 * Session attributes for MFA step-up authentication.
 */
public final class MfaSessionKeys {

    public static final String MFA_VERIFIED = "MFA_VERIFIED";
    public static final String PENDING_SETUP_SECRET = "MFA_PENDING_SETUP_SECRET";

    private MfaSessionKeys() {
    }
}
