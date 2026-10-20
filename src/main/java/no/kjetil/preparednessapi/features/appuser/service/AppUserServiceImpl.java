package no.kjetil.preparednessapi.features.appuser.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.config.security.SecurityProperties;
import no.kjetil.preparednessapi.features.appuser.domain.AppUser;
import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.appuser.dtos.AppUserDto;
import no.kjetil.preparednessapi.features.appuser.dtos.CreateAppUserRequest;
import no.kjetil.preparednessapi.features.articleitem.service.ItemServiceProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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

    @Autowired
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

    @Override
    public Optional<AppUserDto> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Optional<AppUserDto> appUser = fetchAppUser(normalizedEmail);

        return appUser;
    }

    private static AppUserRole parseRole(String role) {
        try {
            return AppUserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + role, e);
        }
    }

    @Override
    public List<AppUserDto> getAllUsers() {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiBaseUrl + BASE_PATH))
                .timeout(Duration.ofSeconds(10))
                .header("apiKey", apiKey)
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new IllegalStateException(String.format("Request %s %s failed. Return code: %d, body: %s",
                        request.method(), request.uri(), response.statusCode(), response.body()));
            }

            List<AppUserDto> users = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            return users;

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AppUser> getAllActiveUsers() {
        String uriPath = BASE_PATH
                + "?active=is.true";

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiBaseUrl + uriPath))
                .timeout(Duration.ofSeconds(10))
                .header("apiKey", apiKey)
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            List<AppUser> users = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            return users;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<AppUserDto> fetchAppUser(String normalizedEmail) {
        String uriPath = BASE_PATH + "?email=eq." + URLEncoder.encode(normalizedEmail, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiBaseUrl + uriPath))
                .timeout(Duration.ofSeconds(10))
                .header("apiKey", apiKey)
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new IllegalStateException(String.format("Request %s %s failed. Return code: %d, body: %s",
                        request.method(), request.uri(), response.statusCode(), response.body()));
            }

            // The rows are returned as an array, which is empty when no user has the email
            List<AppUserDto> users = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            return users.stream().findFirst();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public AppUserDto createAppUser(CreateAppUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("The creation request is missing.");
        }

        if (StringUtils.isBlank(request.email())) {
            throw new IllegalArgumentException("No email provided in the request.");
        }

        if (StringUtils.isBlank(request.role())) {
            throw new IllegalArgumentException("Role is missing in the creation request.");
        }

        // Stored normalized, since lookups by email are normalized the same way
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        NewAppUserRow newUser = new NewAppUserRow(
                Date.from(clock.instant()),
                normalizedEmail,
                parseRole(request.role()),
                true,
                StringUtils.trimToNull(request.name()),
                request.isReceivingEmail() == null || request.isReceivingEmail());

        AppUserDto createdUser = insertUsers(List.of(newUser)).getFirst();

        // A lookup made before the user existed may have cached an empty role
        cache.remove(normalizedEmail);

        return createdUser;
    }

    private List<AppUserDto> insertUsers(List<NewAppUserRow> users) {
        HttpRequest request;
        try {
            request = createPostRequest(BASE_PATH, objectMapper.writeValueAsString(users));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize new app users", e);
        }

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.CONFLICT.value()) {
                throw new AppUserAlreadyExistsException("An app user with that email already exists.");
            }

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new IllegalStateException(String.format("Request %s %s failed. Return code: %d, body: %s",
                        request.method(), request.uri(), response.statusCode(), response.body()));
            }

            // "Prefer: return=representation" makes the database return the saved rows as an array, including the id.
            List<AppUserDto> createdUsers = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            if (createdUsers.size() != users.size()) {
                throw new IllegalStateException(String.format("Expected %d created app users, but got %d",
                        users.size(), createdUsers.size()));
            }
            return createdUsers;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create app users", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating app users", e);
        }
    }

    @Override
    public boolean deleteAppUserById(long id) {
        if (id < 1) {
            throw new IllegalArgumentException("Invalid app user id: " + id);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(apiBaseUrl + BASE_PATH + "?id=eq." + id))
                .timeout(Duration.ofSeconds(10))
                .headers("apiKey", apiKey, "Prefer", "return=representation")
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() < HttpStatus.OK.value()
                    || response.statusCode() >= HttpStatus.MULTIPLE_CHOICES.value()) {
                throw new IllegalStateException(String.format("Request %s %s failed. Return code: %d, body: %s",
                        request.method(), request.uri(), response.statusCode(), response.body()));
            }

            // "Prefer: return=representation" makes the database return the deleted rows, empty if none matched
            List<AppUserDto> deletedUsers = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            // The cache is keyed by email. Revokes access right away instead of when the cached role expires.
            deletedUsers.stream()
                    .map(AppUserDto::email)
                    .filter(Objects::nonNull)
                    .forEach(email -> cache.remove(email.trim().toLowerCase(Locale.ROOT)));

            return !deletedUsers.isEmpty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete app user with id " + id, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while deleting app user with id " + id, e);
        }
    }

    private HttpRequest createPostRequest(String uriPath, String bodyAsJson) {
        return HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(bodyAsJson))
                .uri(URI.create(apiBaseUrl + uriPath))
                .timeout(Duration.ofSeconds(10))
                .headers("apiKey", apiKey,
                        "Content-Type", "application/json",
                        "Prefer", "return=representation")
                .build();
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

    /**
     * The columns sent when inserting a user. The id is left out so the database generates it.
     */
    private record NewAppUserRow(@JsonProperty("created_at") Date createdAt,
                                 String email,
                                 AppUserRole role,
                                 boolean active,
                                 String name,
                                 @JsonProperty("is_receiving_email") boolean isReceivingEmail) {
    }

    private record CachedRole(Optional<AppUserRole> role, Instant expiresAt) {
    }
}
