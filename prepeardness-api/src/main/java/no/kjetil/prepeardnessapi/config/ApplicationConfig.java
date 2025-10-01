package no.kjetil.prepeardnessapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sendgrid.SendGrid;

@Configuration
public class ApplicationConfig {
    @Bean
    public SendGrid sendGrid() {
        return new SendGrid(System.getenv("SEND_GRID_API_KEY"));
    }
}
