package no.kjetil.preparednessapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.http.HttpClient;

@Configuration
public class ApplicationConfig {

    @Bean
    public HttpClient httpClient() {
        // Configure and return an instance of HttpClient
        return HttpClient.newHttpClient(); // Placeholder
    }

    @Bean 
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override 
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/*")
                .allowedOrigins("http://localhost:5173");
            }
        };
    }
}
