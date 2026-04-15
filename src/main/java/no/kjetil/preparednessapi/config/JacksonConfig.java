package no.kjetil.preparednessapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();

        om.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return om;
    }
}
