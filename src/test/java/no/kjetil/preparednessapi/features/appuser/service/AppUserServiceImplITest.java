package no.kjetil.preparednessapi.features.appuser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.DotenvTestInitializer;
import no.kjetil.preparednessapi.config.security.SecurityProperties;
import no.kjetil.preparednessapi.features.appuser.dtos.AppUserDto;
import no.kjetil.preparednessapi.features.articleitem.service.ItemServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = DotenvTestInitializer.class)
public class AppUserServiceImplITest {

    @Autowired
    private ItemServiceProperties serviceProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpClient httpClient;

    private AppUserService sut;

    @BeforeEach
    public void setup() {
        // CI only has placeholder credentials pointing at example.invalid, so only run against a real Supabase
        String url = serviceProperties.getUrl();
        assumeTrue(url != null && !url.isBlank() && !URI.create(url).getHost().endsWith(".invalid"),
                "Requires real Supabase credentials, e.g. from .env");

        SecurityProperties securityProperties = new SecurityProperties(Set.of("Admin@security.com"), Duration.ofMinutes(5));
        sut = new AppUserServiceImpl(
                serviceProperties,
                securityProperties,
                httpClient,
                objectMapper,
                new AppUserServiceImplTest.MutableClock(Instant.now()));
    }

    @Test
    public void shouldBeAbleToGetAllAppUsersFromDatabase() {
        // Act
        List<AppUserDto> allUsers = sut.getAllUsers();

        // Assert
        assertFalse(allUsers.isEmpty());
    }

}
