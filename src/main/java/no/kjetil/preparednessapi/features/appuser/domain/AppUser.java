package no.kjetil.preparednessapi.features.appuser.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record AppUser(long id,
                      @JsonProperty("created_at") Date createdAt,
                      String email,
                      AppUserRole role,
                      boolean active,
                      String name,
                      @JsonProperty("is_receiving_email") boolean isReceivingEmail) {
}
