package no.kjetil.preparednessapi.features.appuser.controllers;

import no.kjetil.preparednessapi.config.ApplicationConfig;
import no.kjetil.preparednessapi.config.security.GoogleJwtAuthenticationConverter;
import no.kjetil.preparednessapi.config.security.SecurityConfig;
import no.kjetil.preparednessapi.features.appuser.domain.AppUserRole;
import no.kjetil.preparednessapi.features.appuser.dtos.AppUserDto;
import no.kjetil.preparednessapi.features.appuser.dtos.CreateAppUserRequest;
import no.kjetil.preparednessapi.features.appuser.service.AppUserAlreadyExistsException;
import no.kjetil.preparednessapi.features.appuser.service.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppUserController.class, properties = {
        "app.security.admins=admin@example.com"
})
@Import({SecurityConfig.class, GoogleJwtAuthenticationConverter.class, ApplicationConfig.class})
class AppUserControllerTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String USER_TOKEN = "user-token";
    private static final String STRANGER_TOKEN = "stranger-token";

    private static final String CREATE_BODY = """
            {"name":"New User","email":"new@example.com","role":"USER","isReceivingEmail":true}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static MockHttpServletRequestBuilder createRequest(String token, String body) {
        return apiRequest(post("/users"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private static MockHttpServletRequestBuilder apiRequest(MockHttpServletRequestBuilder request) {
        return request.header("API-Version", "v1");
    }

    private static MockHttpServletRequestBuilder apiRequest(MockHttpServletRequestBuilder request, String token) {
        return apiRequest(request).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static Jwt googleJwt(String tokenValue, String email) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .issuer("https://accounts.google.com")
                .subject(email)
                .claim("email", email)
                .claim("email_verified", true)
                .build();
    }

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalid token"));
        doReturn(googleJwt(ADMIN_TOKEN, "admin@example.com")).when(jwtDecoder).decode(ADMIN_TOKEN);
        doReturn(googleJwt(USER_TOKEN, "user@example.com")).when(jwtDecoder).decode(USER_TOKEN);
        doReturn(googleJwt(STRANGER_TOKEN, "stranger@example.com")).when(jwtDecoder).decode(STRANGER_TOKEN);

        when(appUserService.findActiveRole(anyString())).thenReturn(Optional.empty());
        when(appUserService.findActiveRole("user@example.com")).thenReturn(Optional.of(AppUserRole.USER));
    }

    @Test
    void adminCanCreateUser() throws Exception {
        CreateAppUserRequest expectedRequest = new CreateAppUserRequest("New User", "new@example.com", "USER", true);
        when(appUserService.createAppUser(expectedRequest))
                .thenReturn(new AppUserDto(42, "new@example.com", "USER", true, "New User", true));

        mockMvc.perform(createRequest(ADMIN_TOKEN, CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void userCannotCreateUser() throws Exception {
        mockMvc.perform(createRequest(USER_TOKEN, CREATE_BODY)).andExpect(status().isForbidden());

        verify(appUserService, never()).createAppUser(any());
    }

    @Test
    void userNotOnAllowlistCannotCreateUser() throws Exception {
        mockMvc.perform(createRequest(STRANGER_TOKEN, CREATE_BODY)).andExpect(status().isForbidden());

        verify(appUserService, never()).createAppUser(any());
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(apiRequest(post("/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());

        verify(appUserService, never()).createAppUser(any());
    }

    @Test
    void adminCanDeleteUserById() throws Exception {
        when(appUserService.deleteAppUserById(42)).thenReturn(true);

        mockMvc.perform(apiRequest(delete("/users/42"), ADMIN_TOKEN)).andExpect(status().isNoContent());
    }

    @Test
    void deletingUnknownUserIsNotFound() throws Exception {
        when(appUserService.deleteAppUserById(404)).thenReturn(false);

        mockMvc.perform(apiRequest(delete("/users/404"), ADMIN_TOKEN)).andExpect(status().isNotFound());
    }

    @Test
    void deletingWithInvalidIdIsBadRequest() throws Exception {
        when(appUserService.deleteAppUserById(0)).thenThrow(new IllegalArgumentException("Invalid app user id: 0"));

        mockMvc.perform(apiRequest(delete("/users/0"), ADMIN_TOKEN)).andExpect(status().isBadRequest());
    }

    @Test
    void deletingWithNonNumericIdIsBadRequest() throws Exception {
        mockMvc.perform(apiRequest(delete("/users/user@example.com"), ADMIN_TOKEN))
                .andExpect(status().isBadRequest());

        verify(appUserService, never()).deleteAppUserById(anyLong());
    }

    @Test
    void userCannotDeleteUsers() throws Exception {
        mockMvc.perform(apiRequest(delete("/users/42"), USER_TOKEN)).andExpect(status().isForbidden());

        verify(appUserService, never()).deleteAppUserById(anyLong());
    }

    @Test
    void deleteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(apiRequest(delete("/users/42"))).andExpect(status().isUnauthorized());

        verify(appUserService, never()).deleteAppUserById(anyLong());
    }

    @Test
    void invalidCreateRequestIsBadRequest() throws Exception {
        when(appUserService.createAppUser(any())).thenThrow(new IllegalArgumentException("Unknown role: OWNER"));

        mockMvc.perform(createRequest(ADMIN_TOKEN, """
                        {"email":"new@example.com","role":"OWNER"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(createRequest(ADMIN_TOKEN, "{not json")).andExpect(status().isBadRequest());

        verify(appUserService, never()).createAppUser(any());
    }

    @Test
    void duplicateUserIsConflict() throws Exception {
        when(appUserService.createAppUser(any())).thenThrow(new AppUserAlreadyExistsException("exists"));

        mockMvc.perform(createRequest(ADMIN_TOKEN, CREATE_BODY)).andExpect(status().isConflict());
    }

    @Test
    void unknownUserByEmailIsNoContent() throws Exception {
        when(appUserService.findUserByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(apiRequest(get("/users/nobody@example.com"), USER_TOKEN))
                .andExpect(status().isNoContent());
    }
}
