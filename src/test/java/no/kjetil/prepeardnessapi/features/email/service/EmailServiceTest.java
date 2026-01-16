package no.kjetil.prepeardnessapi.features.email.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import no.kjetil.prepeardnessapi.DotenvTestInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = DotenvTestInitializer.class)
public class EmailServiceTest {

    @Value("${GMAIL_SMTP_APP_PASSWORD}")
    private String password;

    @Value("${GMAIL_USERNAME:kjetiltorvud@gmail.com}")
    private String username;
    
    private EmailService emailService;
    private String sendGridApiKey;

    @BeforeEach
    public void setup() {
        // Initialize emailService with a mock or actual implementation as needed

        // sendGridApiKey = System.getenv("SEND_GRID_API_KEY");
        // if(sendGridApiKey == null || sendGridApiKey.isEmpty()) {
        //     Properties props = new Properties();
        //     try (InputStream in = getClass().getClassLoader().getResourceAsStream("application-test.properties")) {
        //         if (in != null) {
        //             props.load(in);
        //             sendGridApiKey = props.getProperty("SEND_GRID_API_KEY");
        //         }
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //     }
        // }
        
        emailService = new EmailServiceImpl(username, password);
        
    }

    @Disabled
    @Test
    public void shouldSendEmail() {
        // This is a placeholder test. Implement actual tests with mocking as needed.
        // For example, you could use Mockito to mock the SendGrid client and verify interactions.
        int responseStatus = emailService.sendEmail("kjetiltorvund@gmail.com", "test", "Hello, World!");

        assert(responseStatus == 202); // 202 is the expected status code for a successful SendGrid email request
    }
}
