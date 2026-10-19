package no.kjetil.preparednessapi.features.appuser.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.kjetil.preparednessapi.config.JacksonConfig;
import no.kjetil.preparednessapi.config.security.SecurityProperties;
import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.appuser.dtos.AppUserDto;
import no.kjetil.preparednessapi.features.appuser.dtos.CreateAppUserRequest;
import no.kjetil.preparednessapi.features.articleitem.service.ItemServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppUserServiceImplTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private HttpClient httpClient;
    private MutableClock clock;
    private AppUserServiceImpl appUserService;

    private static String bodyOf(HttpRequest request) {
        HttpRequest.BodyPublisher publisher = request.bodyPublisher().orElseThrow();
        StringBuilder body = new StringBuilder();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                body.append(StandardCharsets.UTF_8.decode(item));
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError(throwable);
            }

            @Override
            public void onComplete() {
            }
        });
        return body.toString();
    }

    @Test
    void shouldUseProductionConstructorWhenCreatedBySpring() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton(
                    "supabaseProperties", new ItemServiceProperties("https://example.supabase.co", "secret-key"));
            context.getBeanFactory().registerSingleton(
                    "securityProperties", new SecurityProperties(Set.of(), Duration.ofMinutes(5)));
            context.getBeanFactory().registerSingleton("httpClient", mock(HttpClient.class));
            context.getBeanFactory().registerSingleton("objectMapper", new JacksonConfig().objectMapper());
            context.register(AppUserServiceImpl.class);
            context.refresh();

            assertNotNull(context.getBean(AppUserServiceImpl.class));
        }
    }

    @Test
    void shouldQueryActiveUserByNormalizedEmail() throws Exception {
        respondWith(200, "[{\"role\":\"USER\"}]");

        Optional<AppUserRole> role = appUserService.findActiveRole(" Some.One+tag@Example.com ");

        assertEquals(Optional.of(AppUserRole.USER), role);
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any(BodyHandler.class));
        HttpRequest request = captor.getValue();
        assertEquals("https://example.supabase.co/rest/v1/app_users"
                        + "?select=role&active=is.true&email=eq.some.one%2Btag%40example.com",
                request.uri().toString());
        assertEquals(Optional.of("secret-key"), request.headers().firstValue("apiKey"));
    }

    @Test
    void shouldReturnAdminRole() throws Exception {
        respondWith(200, "[{\"role\":\"ADMIN\"}]");

        assertEquals(Optional.of(AppUserRole.ADMIN), appUserService.findActiveRole("admin@example.com"));
    }

    @Test
    void shouldReturnEmptyWhenUserIsNotFound() throws Exception {
        respondWith(200, "[]");

        assertEquals(Optional.empty(), appUserService.findActiveRole("stranger@example.com"));
    }

    @Test
    void shouldCacheLookupsUntilTtlHasPassed() throws Exception {
        respondWith(200, "[{\"role\":\"USER\"}]");

        appUserService.findActiveRole("user@example.com");
        clock.advance(Duration.ofMinutes(4));
        appUserService.findActiveRole("USER@example.com");
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(BodyHandler.class));

        clock.advance(Duration.ofMinutes(2));
        appUserService.findActiveRole("user@example.com");
        verify(httpClient, times(2)).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    void shouldCacheUnknownUsers() throws Exception {
        respondWith(200, "[]");

        appUserService.findActiveRole("stranger@example.com");
        appUserService.findActiveRole("stranger@example.com");

        verify(httpClient, times(1)).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    void shouldFailClosedAndNotCacheWhenSupabaseReturnsError() throws Exception {
        respondWith(500, "{\"message\":\"boom\"}");

        assertEquals(Optional.empty(), appUserService.findActiveRole("user@example.com"));

        respondWith(200, "[{\"role\":\"USER\"}]");
        assertEquals(Optional.of(AppUserRole.USER), appUserService.findActiveRole("user@example.com"));
    }

    @Test
    void shouldFailClosedWhenRequestFails() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(new IOException("timeout"));

        assertEquals(Optional.empty(), appUserService.findActiveRole("user@example.com"));
    }

    @Test
    void shouldFailClosedOnUnknownRole() throws Exception {
        respondWith(200, "[{\"role\":\"OWNER\"}]");

        assertEquals(Optional.empty(), appUserService.findActiveRole("user@example.com"));
    }

    @Test
    void shouldNotQueryForBlankEmail() throws Exception {
        assertEquals(Optional.empty(), appUserService.findActiveRole(" "));

        verify(httpClient, never()).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        clock = new MutableClock(Instant.parse("2026-10-07T12:00:00Z"));
        appUserService = new AppUserServiceImpl(
                new ItemServiceProperties("https://example.supabase.co", "secret-key"),
                new SecurityProperties(Set.of(), Duration.ofMinutes(5)),
                httpClient,
                objectMapper,
                clock);
    }

    @Test
    void shouldFindUserByNormalizedEmail() throws Exception {
        respondWith(200, "[{\"id\":7,\"email\":\"user@example.com\",\"role\":\"USER\",\"active\":true,"
                + "\"name\":\"User\",\"is_receiving_email\":true}]");

        Optional<AppUserDto> user = appUserService.findUserByEmail(" USER@example.com ");

        assertEquals(Optional.of(new AppUserDto(7, "user@example.com", "USER", true, "User", true)), user);
        assertEquals("https://example.supabase.co/rest/v1/app_users?email=eq.user%40example.com",
                capturedRequest().uri().toString());
    }

    @Test
    void shouldReturnEmptyWhenNoUserHasTheEmail() throws Exception {
        respondWith(200, "[]");

        assertEquals(Optional.empty(), appUserService.findUserByEmail("stranger@example.com"));
    }

    @Test
    void shouldNotQueryUserForBlankEmail() throws Exception {
        assertEquals(Optional.empty(), appUserService.findUserByEmail(""));
        assertEquals(Optional.empty(), appUserService.findUserByEmail(null));

        verify(httpClient, never()).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    void shouldCreateUserAndReturnTheSavedRow() throws Exception {
        respondWith(201, "[{\"id\":42,\"email\":\"new.user@example.com\",\"role\":\"USER\",\"active\":true,"
                + "\"name\":\"New User\",\"is_receiving_email\":false}]");

        AppUserDto created = appUserService.createAppUser(
                new CreateAppUserRequest("  New User ", " New.User@Example.com ", "USER", false));

        assertEquals(new AppUserDto(42, "new.user@example.com", "USER", true, "New User", false), created);

        HttpRequest request = capturedRequest();
        assertEquals("POST", request.method());
        assertEquals("https://example.supabase.co/rest/v1/app_users", request.uri().toString());
        assertEquals(Optional.of("secret-key"), request.headers().firstValue("apiKey"));
        assertEquals(Optional.of("return=representation"), request.headers().firstValue("Prefer"));
        assertEquals(Optional.of("application/json"), request.headers().firstValue("Content-Type"));

        JsonNode row = objectMapper.readTree(bodyOf(request)).get(0);
        assertFalse(row.has("id"), "The id should be generated by the database");
        assertEquals("new.user@example.com", row.get("email").asText());
        assertEquals("USER", row.get("role").asText());
        assertEquals("New User", row.get("name").asText());
        assertTrue(row.get("active").asBoolean());
        assertFalse(row.get("is_receiving_email").asBoolean());
        assertEquals("2026-10-07T12:00:00.000+00:00", row.get("created_at").asText());
    }

    @Test
    void shouldReceiveEmailByDefaultWhenNotSpecified() throws Exception {
        respondWith(201, "[{\"id\":1,\"email\":\"a@example.com\",\"role\":\"USER\",\"active\":true}]");

        appUserService.createAppUser(new CreateAppUserRequest(null, "a@example.com", "USER", null));

        JsonNode row = objectMapper.readTree(bodyOf(capturedRequest())).get(0);
        assertTrue(row.get("is_receiving_email").asBoolean());
        assertTrue(row.get("name").isNull());
    }

    @Test
    void shouldAcceptRoleRegardlessOfCase() throws Exception {
        respondWith(201, "[{\"id\":1,\"email\":\"a@example.com\",\"role\":\"ADMIN\",\"active\":true}]");

        appUserService.createAppUser(new CreateAppUserRequest("A", "a@example.com", " admin ", true));

        assertEquals("ADMIN", objectMapper.readTree(bodyOf(capturedRequest())).get(0).get("role").asText());
    }

    @Test
    void shouldRejectMissingRequest() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> appUserService.createAppUser(null));

        verify(httpClient, never()).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldRejectBlankEmail(String email) throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> appUserService.createAppUser(new CreateAppUserRequest("A", email, "USER", true)));

        verify(httpClient, never()).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "OWNER", "ROLE_ADMIN"})
    void shouldRejectBlankOrUnknownRole(String role) throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> appUserService.createAppUser(new CreateAppUserRequest("A", "a@example.com", role, true)));

        verify(httpClient, never()).send(any(HttpRequest.class), any(BodyHandler.class));
    }

    @Test
    void shouldReportDuplicateUserWhenSupabaseReturnsConflict() throws Exception {
        respondWith(409, "{\"code\":\"23505\",\"message\":\"duplicate key value\"}");

        assertThrows(AppUserAlreadyExistsException.class, () -> appUserService.createAppUser(
                new CreateAppUserRequest("A", "a@example.com", "USER", true)));
    }

    @Test
    void shouldFailWhenSupabaseReturnsErrorOnCreate() throws Exception {
        respondWith(500, "{\"message\":\"boom\"}");

        assertThrows(IllegalStateException.class, () -> appUserService.createAppUser(
                new CreateAppUserRequest("A", "a@example.com", "USER", true)));
    }

    @Test
    void shouldFailWhenSupabaseReturnsNoCreatedRow() throws Exception {
        respondWith(201, "[]");

        assertThrows(IllegalStateException.class, () -> appUserService.createAppUser(
                new CreateAppUserRequest("A", "a@example.com", "USER", true)));
    }

    @Test
    void shouldFailWhenCreateRequestFails() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(new IOException("timeout"));

        assertThrows(RuntimeException.class, () -> appUserService.createAppUser(
                new CreateAppUserRequest("A", "a@example.com", "USER", true)));
    }

    @Test
    void shouldKeepInterruptFlagWhenCreateIsInterrupted() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
                .thenThrow(new InterruptedException("stop"));

        try {
            assertThrows(RuntimeException.class, () -> appUserService.createAppUser(
                    new CreateAppUserRequest("A", "a@example.com", "USER", true)));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            // Clears the flag so it doesn't leak into other tests
            Thread.interrupted();
        }
    }

    @Test
    void shouldForgetCachedUnknownUserWhenTheUserIsCreated() throws Exception {
        respondWith(200, "[]");
        assertEquals(Optional.empty(), appUserService.findActiveRole("new@example.com"));

        respondWith(201, "[{\"id\":1,\"email\":\"new@example.com\",\"role\":\"USER\",\"active\":true}]");
        appUserService.createAppUser(new CreateAppUserRequest("New", "NEW@example.com", "USER", true));

        respondWith(200, "[{\"role\":\"USER\"}]");
        assertEquals(Optional.of(AppUserRole.USER), appUserService.findActiveRole("new@example.com"));
    }

    private HttpRequest capturedRequest() throws Exception {
        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, atLeastOnce()).send(captor.capture(), any(BodyHandler.class));
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private void respondWith(int statusCode, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
    }

    public static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
