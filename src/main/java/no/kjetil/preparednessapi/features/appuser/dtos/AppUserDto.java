package no.kjetil.preparednessapi.features.appuser.dtos;

public record AppUserDto(long id, String email, String role, boolean active, String name, boolean isReceivingEmail) {
}
