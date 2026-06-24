package com.skyline.org.auth.event;

import com.skyline.org.mail.MailService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthMailEventListenerTest {

    @Test
    void sendsVerificationEmail() {
        MailService mailService = mock(MailService.class);
        AuthMailEventListener listener = new AuthMailEventListener(mailService);
        listener.onVerificationEmail(new VerificationEmailRequested("a@b.com", "http://x", "24h"));
        verify(mailService).sendVerificationEmail("a@b.com", "http://x", "24h");
    }
}
