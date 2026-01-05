package no.kjetil.prepeardnessapi.features.email.service;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.sendgrid.SendGrid;


public class EmailServiceTest {
    
    private EmailService emailService;
    private String sendGridApiKey;


    @BeforeEach
    public void setup() {
        // Initialize emailService with a mock or actual implementation as needed

        sendGridApiKey = System.getenv("SEND_GRID_API_KEY");
        if(sendGridApiKey == null || sendGridApiKey.isEmpty()) {
            Properties props = new Properties();
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("application-test.properties")) {
                if (in != null) {
                    props.load(in);
                    sendGridApiKey = props.getProperty("SEND_GRID_API_KEY");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        emailService = new EmailServiceImpl(new SendGrid(sendGridApiKey));
        
    }

    @Test
    public void shouldSendEmail() {
        // This is a placeholder test. Implement actual tests with mocking as needed.
        // For example, you could use Mockito to mock the SendGrid client and verify interactions.
        int responseStatus = emailService.sendEmail("kjetiltorvund@gmail.com", "test", "Hello, World!");

        assert(responseStatus == 202); // 202 is the expected status code for a successful SendGrid email request
    }
}
