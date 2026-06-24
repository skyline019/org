package com.skyline.org.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.skyline.org.common.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MailServiceTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("from@local", "pass"))
            .withPerMethodLifecycle(false);

    @Test
    void sendsVerificationEmail() throws Exception {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(ServerSetupTest.SMTP.getPort());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "false");

        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.mail.verify.subject", Locale.getDefault(), "Verify");
        source.addMessage("auth.mail.verify.body", Locale.getDefault(), "Expires {0}: {1}");
        MailService mailService = new MailService(sender, new Messages(source), "from@local");

        mailService.sendVerificationEmail("to@local", "http://verify", "24h");
        assertThat(GREEN_MAIL.getReceivedMessages()).hasSize(1);
        assertThat(GreenMailUtil.getBody(GREEN_MAIL.getReceivedMessages()[0])).contains("http://verify");
    }
}
