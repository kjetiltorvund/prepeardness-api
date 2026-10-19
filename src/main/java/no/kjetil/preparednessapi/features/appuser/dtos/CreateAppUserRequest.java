package no.kjetil.preparednessapi.features.appuser.dtos;

public record CreateAppUserRequest(String name, String email, String role, boolean isReceivingEmail) {
}
