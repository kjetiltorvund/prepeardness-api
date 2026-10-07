package no.kjetil.preparednessapi.features.appuser.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.config.security.SecurityProperties;
import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.articleitem.service.ItemServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up users in the Supabase table app_users.
 * Lookups are cached for a short time, so a role change or removal takes effect after at most the cache TTL.
 */
@Service
public class AppUserServiceImpl implements AppUserService {

    private static final Logger logger = LoggerFactory.getLogger(AppUserServiceImpl.class);
    private static final String BASE_PATH = "/rest/v1/app_users";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String apiKey;
    private final Duration cacheTtl;
    private final Clock clock;

    private final Map<String, CachedRole> cache = new ConcurrentHashMap<>();

    public AppUserServiceImpl(ItemServiceProperties supabaseProperties,
                              SecurityProperties securityProperties,
                              HttpClient httpClient,
                              ObjectMapper objectMapper) {
        this(supabaseProperties, securityProperties, httpClient, objectMapper, Clock.systemUTC());
    }

    AppUserServiceImpl(ItemServiceProperties supabaseProperties,
                       SecurityProperties securityProperties,
                       HttpClient httpClient,
                       ObjectMapper objectMapper,
                       Clock clock) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiBaseUrl = supabaseProperties.getUrl();
        this.apiKey = supabaseProperties.getApiKey();
        this.cacheTtl = securityProperties.userCacheTtl();
        this.clock = clock;
    }

    @Override
    public Optional<AppUserRole> findActiveRole(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Instant now = clock.instant();

        CachedRole cached = cache.get(normalizedEmail);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.role();
        }

        try {
            Optional<AppUserRole> role = fetchActiveRole(normalizedEmail);
            cache.put(normalizedEmail, new CachedRole(role, now.plus(cacheTtl)));
            return role;
        } catch (IOException | RuntimeException e) {
            // Fail closed, and don't cache the failure so the next request tries again
            logger.error("Unable to look up app user {}: {}", normalizedEmail, e.getMessage(), e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while looking up app user {}", normalizedEmail, e);
            return Optional.empty();
        }
    }

    private Optional<AppUserRole> fetchActiveRole(String email) throws IOException, InterruptedException {
        String uriPath = BASE_PATH
                + "?select=role"
                + "&active=is.true"
                + "&email=eq." + URLEncoder.encode(email, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiBaseUrl + uriPath))
                .timeout(Duration.ofSeconds(10))
                .header("apiKey", apiKey)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < HttpStatus.OK.value()
                || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
            throw new IllegalStateException(String.format("Request %s %s failed. Return code: %d, body: %s",
                    request.method(), request.uri(), response.statusCode(), response.body()));
        }

        List<AppUserRow> rows = objectMapper.readValue(response.body(), new TypeReference<>() {
        });

        return rows.stream()
                .findFirst()
                .map(row -> AppUserRole.valueOf(row.role()));
    }

    private record AppUserRow(String role) {
    }

    private record CachedRole(Optional<AppUserRole> role, Instant expiresAt) {
    }
}
