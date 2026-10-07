package no.kjetil.preparednessapi.features.articleitem.controller;

import no.kjetil.preparednessapi.config.ApplicationConfig;
import no.kjetil.preparednessapi.config.ModelMapperConfig;
import no.kjetil.preparednessapi.config.security.GoogleJwtAuthenticationConverter;
import no.kjetil.preparednessapi.config.security.SecurityConfig;
import no.kjetil.preparednessapi.features.articleitem.domain.ArticleItem;
import no.kjetil.preparednessapi.features.articleitem.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class, properties = {
        "app.security.admins=admin@example.com",
        "app.security.users=user@example.com"
})
@Import({SecurityConfig.class, GoogleJwtAuthenticationConverter.class, ApplicationConfig.class, ModelMapperConfig.class})
class ItemControllerSecurityTest {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String USER_TOKEN = "user-token";
    private static final String STRANGER_TOKEN = "stranger-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalid token"));
        doReturn(googleJwt(ADMIN_TOKEN, "admin@example.com")).when(jwtDecoder).decode(ADMIN_TOKEN);
        doReturn(googleJwt(USER_TOKEN, "user@example.com")).when(jwtDecoder).decode(USER_TOKEN);
        doReturn(googleJwt(STRANGER_TOKEN, "stranger@example.com")).when(jwtDecoder).decode(STRANGER_TOKEN);

        when(itemService.findAllByExpirationDateAndReplaced(null, false)).thenReturn(List.of());
        when(itemService.findById(1L)).thenReturn(ArticleItem.builder().id(1L).build());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(apiRequest(get("/items"))).andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(apiRequest(get("/items"), "garbage")).andExpect(status().isUnauthorized());
    }

    @Test
    void userNotOnAllowlistIsForbidden() throws Exception {
        mockMvc.perform(apiRequest(get("/items"), STRANGER_TOKEN)).andExpect(status().isForbidden());
    }

    @Test
    void userCanReadItems() throws Exception {
        mockMvc.perform(apiRequest(get("/items"), USER_TOKEN)).andExpect(status().isOk());
    }

    @Test
    void userCannotDeleteItems() throws Exception {
        mockMvc.perform(apiRequest(delete("/items/1"), USER_TOKEN)).andExpect(status().isForbidden());
    }

    @Test
    void userCannotBatchCreateItems() throws Exception {
        mockMvc.perform(apiRequest(post("/items/batch"), USER_TOKEN)
                        .contentType("application/json")
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteItems() throws Exception {
        mockMvc.perform(apiRequest(delete("/items/1"), ADMIN_TOKEN)).andExpect(status().isNoContent());
    }

    @Test
    void corsPreflightIsAllowedWithoutToken() throws Exception {
        mockMvc.perform(options("/items")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,API-Version"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
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
}
