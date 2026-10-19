package no.kjetil.preparednessapi.features.appuser.dtos;

/**
 * @param isReceivingEmail defaults to true when left out
 */
public record CreateAppUserRequest(String name, String email, String role, Boolean isReceivingEmail) {
}
