package no.kjetil.preparednessapi.features.appuser.controllers;

import no.kjetil.preparednessapi.features.appuser.dtos.AppUserDto;
import no.kjetil.preparednessapi.features.appuser.dtos.CreateAppUserRequest;
import no.kjetil.preparednessapi.features.appuser.service.AppUserAlreadyExistsException;
import no.kjetil.preparednessapi.features.appuser.service.AppUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/users", headers = "API-Version=v1")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public ResponseEntity<List<AppUserDto>> getAllUsers() {

        List<AppUserDto> users = appUserService.getAllUsers();

        return ResponseEntity.ok(users);
    }

    @GetMapping("{email}")
    public ResponseEntity<AppUserDto> getUserByEmail(@PathVariable String email) {

        Optional<AppUserDto> appUser = appUserService.findUserByEmail(email);

        return appUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());

    }

    @PostMapping
    public ResponseEntity<AppUserDto> createAppUser(@RequestBody CreateAppUserRequest request) {
        try {
            AppUserDto createdUser = appUserService.createAppUser(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (AppUserAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
