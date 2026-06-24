package com.skyline.org.auth.controller;

import com.skyline.org.auth.dto.AvailabilityResponse;
import com.skyline.org.auth.dto.PasswordCheckResponse;
import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.dto.ResendVerificationRequest;
import com.skyline.org.auth.service.AvailabilityCheckService;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordCheckService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth API", description = "Registration and dynamic validation endpoints")
public class AuthApiController {

    private final AvailabilityCheckService availabilityCheckService;
    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordCheckService passwordCheckService;
    private final Messages messages;

    public AuthApiController(
            AvailabilityCheckService availabilityCheckService,
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            PasswordCheckService passwordCheckService,
            Messages messages) {
        this.availabilityCheckService = availabilityCheckService;
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.passwordCheckService = passwordCheckService;
        this.messages = messages;
    }

    @GetMapping("/check/username")
    @Operation(summary = "Check username format and availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkUsername(@RequestParam String value) {
        AvailabilityResponse response = availabilityCheckService.checkUsername(value);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/check/email")
    @Operation(summary = "Check email format and availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkEmail(@RequestParam String value) {
        AvailabilityResponse response = availabilityCheckService.checkEmail(value);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/check/password")
    @Operation(summary = "Check password strength")
    public ResponseEntity<ApiResponse<PasswordCheckResponse>> checkPassword(@RequestParam String value) {
        return ResponseEntity.ok(ApiResponse.ok(passwordCheckService.check(value)));
    }

    @PostMapping("/register")
    @Operation(summary = "Register via JSON API")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(null, messages.get("auth.resend.success")));
    }
}
