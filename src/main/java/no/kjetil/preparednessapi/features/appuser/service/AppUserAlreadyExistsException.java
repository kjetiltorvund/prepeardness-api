package no.kjetil.preparednessapi.features.appuser.service;

public class AppUserAlreadyExistsException extends RuntimeException {

    public AppUserAlreadyExistsException(String message) {
        super(message);
    }
}
