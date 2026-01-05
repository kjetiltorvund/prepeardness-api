package no.kjetil.prepeardnessapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prepeardness API")
                        .version("1.0")
                        .description("Prepeardness API will help register grocery items, register their expiration date and notify all users when the expiration date is soon too expire or has expired.")
                );
    }
}
