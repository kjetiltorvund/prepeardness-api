package no.kjetil.preparednessapi.features.appuser.service;

import no.kjetil.preparednessapi.config.JacksonConfig;
import no.kjetil.preparednessapi.config.security.SecurityProperties;
import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.articleitem.service.ItemServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppUserServiceImplTest {

    private HttpClient httpClient;
    private MutableClock clock;
    private AppUserServiceImpl appUserService;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        clock = new MutableClock(Instant.parse("2026-10-07T12:00:00Z"));
        appUserService = new AppUserServiceImpl(
                new ItemServiceProperties("https://example.supabase.co", "secret-key"),
                new SecurityProperties(Set.of(), Duration.ofMinutes(5)),
                httpClient,
                new JacksonConfig().objectMapper(),
                clock);
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

    @SuppressWarnings("unchecked")
    private void respondWith(int statusCode, String body) throws Exception {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
    }

    private static class MutableClock extends Clock {
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
