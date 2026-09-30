package no.kjetil.preparednessapi.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.http.HttpClient;

@Configuration
@EnableCaching 
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
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", "https://kjetiltorvund.github.io")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders(
                                "API-Version",
                                "Content-Type",
                                "X-App-Version",
                                "Authorization");
            }
        };
    }

    @Bean 
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("articles");
    }
}
