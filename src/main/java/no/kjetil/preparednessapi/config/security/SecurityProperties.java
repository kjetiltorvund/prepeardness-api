package no.kjetil.preparednessapi.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allowlists of Google account emails that may use the API.
 * Emails are compared case-insensitively.
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(Set<String> admins, Set<String> users) {

    public SecurityProperties {
        admins = normalize(admins);
        users = normalize(users);
    }

    public boolean isAdmin(String email) {
        return email != null && admins.contains(email.toLowerCase(Locale.ROOT));
    }

    public boolean isUser(String email) {
        return email != null && users.contains(email.toLowerCase(Locale.ROOT));
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
