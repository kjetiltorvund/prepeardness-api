package no.kjetil.preparednessapi.features.appuser.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AppUserDto(long id,
                         String email,
                         String role,
                         boolean active,
                         String name,
                         // Supabase returns the column as is_receiving_email
                         @JsonAlias("is_receiving_email") boolean isReceivingEmail) {
}
