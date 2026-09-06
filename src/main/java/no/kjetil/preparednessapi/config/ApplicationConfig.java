package no.kjetil.preparednessapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class ApplicationConfig {

    @Bean
    public HttpClient httpClient() {
        // Configure and return an instance of HttpClient
        return HttpClient.newHttpClient(); // Placeholder
    }
}
