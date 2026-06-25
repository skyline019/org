package com.skyline.org.auth.controller;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.auth.mfa.MfaEnrollmentResult;
import com.skyline.org.auth.mfa.MfaRecoveryCodeService;
import com.skyline.org.auth.mfa.MfaService;
import com.skyline.org.auth.mfa.MfaSessionKeys;
import com.skyline.org.auth.mfa.TotpMfaService;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.web.ClientIpResolver;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth/mfa")
@ConditionalOnProperty(name = "app.auth.mfa.enabled", havingValue = "true")
public class MfaController {

    private final MfaService mfaService;
    private final TotpMfaService totpMfaService;
    private final UserService userService;
    private final AuthAuditService authAuditService;
    private final AuthProperties authProperties;
    private final ClientIpResolver clientIpResolver;
    private final Messages messages;

    public MfaController(
            MfaService mfaService,
            TotpMfaService totpMfaService,
            UserService userService,
            AuthAuditService authAuditService,
            AuthProperties authProperties,
            ClientIpResolver clientIpResolver,
            Messages messages) {
        this.mfaService = mfaService;
        this.totpMfaService = totpMfaService;
        this.userService = userService;
        this.authAuditService = authAuditService;
        this.authProperties = authProperties;
        this.clientIpResolver = clientIpResolver;
        this.messages = messages;
    }

    @GetMapping("/challenge")
    public String challengePage(@AuthenticationPrincipal UserDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (!mfaService.requiresChallenge(principal.getUsername())) {
            return "redirect:" + authProperties.getAuth().getLoginSuccessUrl();
        }
        return "auth/mfa-challenge";
    }

    @PostMapping("/challenge")
    public String verifyChallenge(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String code,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getUsername();
        String ip = clientIpResolver.resolve(request);
        if (mfaService.verifyChallenge(username, code)) {
            HttpSession session = request.getSession(true);
            session.setAttribute(MfaSessionKeys.MFA_VERIFIED, Boolean.TRUE);
            String detail = MfaRecoveryCodeService.looksLikeRecoveryCode(code) ? "recovery" : null;
            authAuditService.log(AuthEventType.MFA_CHALLENGE_SUCCESS, username, ip, detail);
            authAuditService.log(AuthEventType.LOGIN_SUCCESS, username, ip, detail == null ? "mfa" : "mfa-recovery");
            redirectAttributes.addFlashAttribute("successMessage", msg("auth.mfa.challenge.success"));
            return "redirect:" + authProperties.getAuth().getLoginSuccessUrl();
        }
        authAuditService.log(AuthEventType.MFA_CHALLENGE_FAILURE, username, ip, null);
        redirectAttributes.addFlashAttribute("errorMessage", msg("auth.mfa.challenge.error"));
        return "redirect:/auth/mfa/challenge";
    }

    @GetMapping("/setup")
    public String setupPage(@AuthenticationPrincipal UserDetails principal, Model model) throws QrGenerationException {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getUsername();
        model.addAttribute("mandatory", mfaService.requiresMandatoryEnrollment(username, principal.getAuthorities()));
        if (mfaService.isEnrolled(username)) {
            model.addAttribute("alreadyEnrolled", true);
            return "auth/mfa-setup";
        }
        User user = userService.findByUsername(username).orElseThrow();
        String secret = mfaService.beginEnrollment(username);
        model.addAttribute("secret", secret);
        model.addAttribute("qrCodeDataUri", totpMfaService.buildQrDataUri(user.getEmail(), secret));
        return "auth/mfa-setup";
    }

    @PostMapping("/enable")
    public String enableMfa(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String code,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getUsername();
        String ip = clientIpResolver.resolve(request);
        try {
            MfaEnrollmentResult result = mfaService.confirmEnrollment(username, code);
            request.getSession(true).setAttribute(MfaSessionKeys.MFA_VERIFIED, Boolean.TRUE);
            authAuditService.log(AuthEventType.MFA_ENROLLED, username, ip, null);
            redirectAttributes.addFlashAttribute("recoveryCodes", result.recoveryCodes());
            redirectAttributes.addFlashAttribute("successMessage", msg("auth.mfa.enroll.success"));
            return "redirect:/auth/mfa/recovery-codes";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", msg("auth.mfa.challenge.error"));
            return "redirect:/auth/mfa/setup";
        }
    }

    @GetMapping("/recovery-codes")
    public String recoveryCodesPage(
            @AuthenticationPrincipal UserDetails principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (!model.containsAttribute("recoveryCodes")) {
            return "redirect:/auth/mfa/setup";
        }
        return "auth/mfa-recovery-codes";
    }

    @PostMapping("/disable")
    public String disableMfa(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam String code,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getUsername();
        if (mfaService.requiresMandatoryEnrollment(username, principal.getAuthorities())) {
            redirectAttributes.addFlashAttribute("errorMessage", msg("auth.mfa.disable.forbidden"));
            return "redirect:/auth/mfa/setup";
        }
        if (!mfaService.verifyChallenge(username, code)) {
            redirectAttributes.addFlashAttribute("errorMessage", msg("auth.mfa.challenge.error"));
            return "redirect:/auth/mfa/setup";
        }
        mfaService.disable(username);
        request.getSession(true).removeAttribute(MfaSessionKeys.MFA_VERIFIED);
        redirectAttributes.addFlashAttribute("successMessage", msg("auth.mfa.disable.success"));
        return "redirect:/auth/mfa/setup";
    }

    private String msg(String key) {
        return messages.get(key);
    }
}
