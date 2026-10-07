package no.kjetil.preparednessapi.features.appuser.service;

import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;

import java.util.Optional;

public interface AppUserService {
    /**
     * @return the role of the active user with the given email, or empty if there is none
     */
    Optional<AppUserRole> findActiveRole(String email);
}
