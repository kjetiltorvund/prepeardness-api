package no.kjetil.preparednessapi.features.email.service;

import no.kjetil.preparednessapi.DotenvTestInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = DotenvTestInitializer.class)
public class EmailServiceTest {

    @Value("${GMAIL_SMTP_APP_PASSWORD}")
    private String password;

    @Value("${GMAIL_USERNAME:kjetiltorvud@gmail.com}")
    private String username;
    
    private EmailService emailService;

    @BeforeEach
    public void setup() {
        EmailProperties emailProperties = new EmailProperties(username, password);
        
        emailService = new EmailServiceImpl(emailProperties);
        
    }

    @Disabled
    @Test
    public void shouldSendEmail() {
        int responseStatus = emailService.sendEmail("kjetiltorvund@gmail.com", "test", "Hello, World!");

        assert(responseStatus == 1);
    }
}
