package com.skyline.org.auth.mfa;

import java.util.List;

public record MfaEnrollmentResult(List<String> recoveryCodes) {
}
