package no.kjetil.preparednessapi.config.security;

import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.appuser.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GoogleJwtAuthenticationConverterTest {

    private final AppUserService appUserService = mock(AppUserService.class);

    private final GoogleJwtAuthenticationConverter converter = new GoogleJwtAuthenticationConverter(
            new SecurityProperties(Set.of(" Admin@Example.com "), null), appUserService);

    @Test
    void configuredAdminGetsAdminAndUserRolesWithoutDatabaseLookup() {
        AbstractAuthenticationToken token = converter.convert(googleJwt("ADMIN@example.com", true));

        assertEquals(Set.of("ROLE_ADMIN", "ROLE_USER"), roles(token));
        assertEquals("ADMIN@example.com", token.getName());
        verifyNoInteractions(appUserService);
    }

    @Test
    void adminFromDatabaseGetsAdminAndUserRoles() {
        when(appUserService.findActiveRole("dbadmin@example.com")).thenReturn(Optional.of(AppUserRole.ADMIN));

        assertEquals(Set.of("ROLE_ADMIN", "ROLE_USER"), roles(converter.convert(googleJwt("dbadmin@example.com", true))));
    }

    @Test
    void userFromDatabaseGetsUserRole() {
        when(appUserService.findActiveRole("user@example.com")).thenReturn(Optional.of(AppUserRole.USER));

        assertEquals(Set.of("ROLE_USER"), roles(converter.convert(googleJwt("user@example.com", true))));
    }

    @Test
    void unknownEmailGetsNoRoles() {
        when(appUserService.findActiveRole("stranger@example.com")).thenReturn(Optional.empty());

        assertTrue(roles(converter.convert(googleJwt("stranger@example.com", true))).isEmpty());
    }

    @Test
    void unverifiedEmailGetsNoRoles() {
        assertTrue(roles(converter.convert(googleJwt("admin@example.com", false))).isEmpty());
        verifyNoInteractions(appUserService);
    }

    @Test
    void missingEmailGetsNoRolesAndUsesSubjectAsName() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("12345").build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertTrue(roles(token).isEmpty());
        assertEquals("12345", token.getName());
    }

    private static Jwt googleJwt(String email, boolean emailVerified) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("12345")
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .build();
    }

    private static Set<String> roles(AbstractAuthenticationToken token) {
        return token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
    }
}
