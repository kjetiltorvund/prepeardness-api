package no.kjetil.preparednessapi.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @param admins       emails that always get the admin role, also when the app_users table is unavailable.
 *                     Compared case-insensitively.
 * @param userCacheTtl how long a lookup in the app_users table is cached
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Set<String> admins, Duration userCacheTtl) {

    public SecurityProperties {
        admins = normalize(admins);
        if (userCacheTtl == null) {
            userCacheTtl = Duration.ofMinutes(5);
        }
    }

    public boolean isAdmin(String email) {
        return email != null && admins.contains(email.toLowerCase(Locale.ROOT));
    }

    private static Set<String> normalize(Set<String> emails) {
        if (emails == null) {
            return Set.of();
        }
        return emails.stream()
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
