package no.kjetil.preparednessapi.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleJwtAuthenticationConverterTest {

    private final GoogleJwtAuthenticationConverter converter = new GoogleJwtAuthenticationConverter(
            new SecurityProperties(Set.of(" Admin@Example.com "), Set.of("user@example.com")));

    @Test
    void adminGetsAdminAndUserRoles() {
        AbstractAuthenticationToken token = converter.convert(googleJwt("admin@example.com", true));

        assertEquals(Set.of("ROLE_ADMIN", "ROLE_USER"), roles(token));
        assertEquals("admin@example.com", token.getName());
    }

    @Test
    void emailIsComparedCaseInsensitively() {
        assertEquals(Set.of("ROLE_USER"), roles(converter.convert(googleJwt("USER@example.com", true))));
    }

    @Test
    void unknownEmailGetsNoRoles() {
        assertTrue(roles(converter.convert(googleJwt("stranger@example.com", true))).isEmpty());
    }

    @Test
    void unverifiedEmailGetsNoRoles() {
        assertTrue(roles(converter.convert(googleJwt("admin@example.com", false))).isEmpty());
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
