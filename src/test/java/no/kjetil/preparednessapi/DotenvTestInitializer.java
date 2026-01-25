package no.kjetil.preparednessapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * ApplicationContextInitializer to load environment variables from a .env file for testing purposes.
 */
public class DotenvTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        Map<String, Object> props = new HashMap<>();
        dotenv.entries().forEach(e -> props.put(e.getKey(), e.getValue()));

        MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
        // Add first so these can override defaults
        sources.addFirst(new MapPropertySource("dotenv", props));
    }
}
