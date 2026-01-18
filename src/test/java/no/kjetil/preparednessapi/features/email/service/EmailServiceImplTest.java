package no.kjetil.preparednessapi.features.email.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import no.kjetil.preparednessapi.DotenvTestInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = DotenvTestInitializer.class)
class EmailServiceImplTest {

    @Value("${GMAIL_SMTP_APP_PASSWORD}")
    private String password;

    @Value("${GMAIL_USERNAME:kjetiltorvud@gmail.com}")
    private String username;

    @Disabled("Only need to test this once or manually")
    @Test
    public void shouldSendEmailToRecipient() {

        EmailService emailService = new EmailServiceImpl(username, password);

        Assertions.assertDoesNotThrow(() -> {
            emailService.sendEmail("kjetiltorvund@gmail.com", "Testing", "Testing");
        }, "An unexpected exception was thrown!");
    }
}