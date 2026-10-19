package no.kjetil.preparednessapi.features.appuser.service;

import no.kjetil.preparednessapi.features.appuser.domain.AppUser;
import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.appuser.dtos.AppUserDto;

import java.util.List;
import java.util.Optional;

public interface AppUserService {
    /**
     * @return the role of the active user with the given email, or empty if there is none
     */
    Optional<AppUserRole> findActiveRole(String email);

    Optional<AppUserDto> findUserByEmail(String email);

    List<AppUserDto> getAllUsers();

    List<AppUser> getAllActiveUsers();
}
