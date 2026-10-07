package no.kjetil.preparednessapi.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a validated Google ID token to roles, based on the email allowlists in {@link SecurityProperties}.
 * Users that are not on any allowlist are authenticated without roles, and are therefore rejected with 403.
 */
@Component
public class GoogleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String ROLE_ADMIN = "ROLE_ADMIN";
    static final String ROLE_USER = "ROLE_USER";

    private final SecurityProperties securityProperties;

    public GoogleJwtAuthenticationConverter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String principalName = email != null ? email : jwt.getSubject();

        return new JwtAuthenticationToken(jwt, authorities(jwt, email), principalName);
    }

    private List<GrantedAuthority> authorities(Jwt jwt, String email) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Google only guarantees ownership of the address when it is verified
        if (email == null || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            return authorities;
        }

        if (securityProperties.isAdmin(email)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        } else if (securityProperties.isUser(email)) {
            authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        }
        return authorities;
    }
}
