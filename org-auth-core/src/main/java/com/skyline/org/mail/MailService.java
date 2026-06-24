package com.skyline.org.mail;

import com.skyline.org.common.i18n.Messages;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final Messages messages;
    private final String from;

    public MailService(
            JavaMailSender mailSender,
            Messages messages,
            @Value("${spring.mail.from:${spring.mail.username:}}") String from) {
        this.mailSender = mailSender;
        this.messages = messages;
        this.from = from;
    }

    public void sendVerificationEmail(String to, String verificationUrl, String expiryDescription) {
        SimpleMailMessage message = buildMessage(
                to,
                messages.get("auth.mail.verify.subject"),
                messages.get("auth.mail.verify.body", expiryDescription, verificationUrl));
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String resetUrl, String expiryDescription) {
        SimpleMailMessage message = buildMessage(
                to,
                messages.get("auth.mail.reset.subject"),
                messages.get("auth.mail.reset.body", expiryDescription, resetUrl));
        mailSender.send(message);
    }

    private SimpleMailMessage buildMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }
}
