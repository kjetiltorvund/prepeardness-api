package no.kjetil.preparednessapi.config.security;

import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.appuser.service.AppUserService;
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
 * Maps a validated Google ID token to roles. Emails in {@link SecurityProperties#admins()} are always admins,
 * everyone else gets the role registered in the app_users table.
 * Users that are not found are authenticated without roles, and are therefore rejected with 403.
 */
@Component
public class GoogleJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String ROLE_ADMIN = "ROLE_ADMIN";
    static final String ROLE_USER = "ROLE_USER";

    private final SecurityProperties securityProperties;
    private final AppUserService appUserService;

    public GoogleJwtAuthenticationConverter(SecurityProperties securityProperties, AppUserService appUserService) {
        this.securityProperties = securityProperties;
        this.appUserService = appUserService;
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

        AppUserRole role = securityProperties.isAdmin(email)
                ? AppUserRole.ADMIN
                : appUserService.findActiveRole(email).orElse(null);

        if (role == AppUserRole.ADMIN) {
            authorities.add(new SimpleGrantedAuthority(ROLE_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        } else if (role == AppUserRole.USER) {
            authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        }
        return authorities;
    }
}
