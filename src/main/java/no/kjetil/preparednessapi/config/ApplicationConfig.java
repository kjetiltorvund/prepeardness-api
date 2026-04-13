package no.kjetil.preparednessapi.config;

import com.sendgrid.SendGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class ApplicationConfig {
    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(System.getenv("SEND_GRID_API_KEY"));
    }

    @Bean
    public HttpClient httpClient() {
        // Configure and return an instance of HttpClient
        return HttpClient.newHttpClient(); // Placeholder
    }
}
