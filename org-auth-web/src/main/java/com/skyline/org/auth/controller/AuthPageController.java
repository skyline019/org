package com.skyline.org.auth.controller;

import com.skyline.org.auth.dto.ForgotPasswordRequest;
import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.auth.dto.ResetPasswordRequest;
import com.skyline.org.auth.service.EmailVerificationService;
import com.skyline.org.auth.service.PasswordResetService;
import com.skyline.org.auth.service.RegistrationService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthPageController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final Messages messages;

    public AuthPageController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService,
            Messages messages) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.messages = messages;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null && !model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", msg("auth.login.error"));
        }
        if (logout != null) {
            model.addAttribute("successMessage", msg("auth.login.logout"));
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            registrationService.register(request);
            redirectAttributes.addFlashAttribute("registeredEmail", request.getEmail());
            return "redirect:/auth/verify-email-pending";
        } catch (BusinessException ex) {
            bindingResult.reject("register", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/auth/verify-email-pending")
    public String verifyEmailPending(Model model) {
        if (!model.containsAttribute("registeredEmail")) {
            return "redirect:/register";
        }
        return "auth/verify-email-pending";
    }

    @PostMapping("/auth/resend-verification")
    public String resendVerification(
            @RequestParam String email,
            RedirectAttributes redirectAttributes) {
        emailVerificationService.resendVerificationEmail(email);
        redirectAttributes.addFlashAttribute("registeredEmail", email);
        redirectAttributes.addFlashAttribute("successMessage", msg("auth.resend.success"));
        return "redirect:/auth/verify-email-pending";
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String forgotPassword(
            @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        passwordResetService.requestReset(request.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", msg("auth.forgot.success"));
        return "redirect:/login";
    }

    @GetMapping("/auth/reset-password")
    public String resetPasswordLegacy(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return "redirect:/auth/forgot-password";
        }
        return "redirect:/auth/reset-password/" + token;
    }

    @GetMapping("/auth/reset-password/{token}")
    public String resetPasswordPage(@PathVariable String token, Model model) {
        if (!passwordResetService.isTokenValid(token)) {
            model.addAttribute("errorMessage", msg("auth.reset.invalid-link"));
            return "auth/reset-password-error";
        }
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        model.addAttribute("resetPasswordRequest", request);
        return "auth/reset-password";
    }

    @PostMapping("/auth/reset-password")
    public String resetPassword(
            @Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            passwordResetService.resetPassword(request);
            redirectAttributes.addFlashAttribute("successMessage", msg("auth.reset.success"));
            return "redirect:/login";
        } catch (BusinessException ex) {
            bindingResult.reject("reset", ex.getMessage());
            return "auth/reset-password";
        }
    }

    @GetMapping("/auth/verify-email")
    public String verifyEmailLegacy(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return "redirect:/login";
        }
        return "redirect:/auth/verify-email/" + token;
    }

    @GetMapping("/auth/verify-email/{token}")
    public String verifyEmailConfirmPage(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "auth/verify-email-confirm";
    }

    @PostMapping("/auth/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            emailVerificationService.verifyEmail(token);
            model.addAttribute("successMessage", msg("auth.verify.success"));
            return "auth/verify-email-result";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/verify-email-result";
        }
    }

    private String msg(String key) {
        return messages.get(key);
    }
}
