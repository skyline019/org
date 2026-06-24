package com.skyline.org.auth.event;

import com.skyline.org.mail.MailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuthMailEventListener {

    private final MailService mailService;

    public AuthMailEventListener(MailService mailService) {
        this.mailService = mailService;
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationEmail(VerificationEmailRequested event) {
        mailService.sendVerificationEmail(event.email(), event.verificationUrl(), event.expiryDescription());
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmail(PasswordResetEmailRequested event) {
        mailService.sendPasswordResetEmail(event.email(), event.resetUrl(), event.expiryDescription());
    }
}
