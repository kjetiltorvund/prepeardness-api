package no.kjetil.prepeardnessapi.features.email.service;

import no.kjetil.prepeardnessapi.utils.DotenvLoader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class EmailServiceImplPlainTest {

    @Disabled("Only need to test this once or manually")
    @Test
    public void shouldSendEmailWithoutSpringContext() {
        String username = DotenvLoader.getOrDefault("GMAIL_USERNAME", "fallback@example.com");
        String password = DotenvLoader.getOrDefault("GMAIL_SMTP_APP_PASSWORD", "dummy");

        EmailService emailService = new EmailServiceImpl(username, password);

        assertDoesNotThrow(() -> emailService.sendEmail("kjetiltorvund@gmail.com", "Testing"));
    }
}
