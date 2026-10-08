package com.carbuyer.infrastructure.security.controller;

import com.carbuyer.infrastructure.security.JwtService;
import com.carbuyer.infrastructure.security.dto.AuthResponse;
import com.carbuyer.infrastructure.security.dto.LoginRequest;
import com.carbuyer.infrastructure.security.dto.RefreshTokenRequest;
import com.carbuyer.infrastructure.security.dto.RegisterRequest;
import com.carbuyer.infrastructure.security.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService service;
    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_withInvalidEmailOrShortPassword_shouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("user", "invalid-email", "pass");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void login_withInvalidEmailOrShortPassword_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", "pass");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void refresh_withInvalidToken_shouldReturn400() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("  ");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void register_withValidRequest_shouldReturn201WithTokens() throws Exception {
        RegisterRequest request = new RegisterRequest("user", "user@mail.com", "password");
        String expectedRefreshToken = "refreshToken";
        String expectedAccessToken = "accessToken";
        when(service.register(request))
                .thenReturn(new AuthResponse(expectedAccessToken, expectedRefreshToken));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        AuthResponse authResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(result).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isEqualTo(expectedAccessToken);
        assertThat(authResponse.refreshToken()).isEqualTo(expectedRefreshToken);
    }

    @Test
    void login_withValidRequest_shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest("user@mail.com", "password");
        String expectedAccessToken = "accessToken";
        String expectedRefreshToken = "refreshToken";
        when(service.login(request))
                .thenReturn(new AuthResponse(expectedAccessToken, expectedRefreshToken));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        AuthResponse authResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(result).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isEqualTo(expectedAccessToken);
        assertThat(authResponse.refreshToken()).isEqualTo(expectedRefreshToken);
    }

    @Test
    void refresh_withValidToken_shouldReturn200WithTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refreshToken");
        String expectedAccessToken = "accessToken";
        String expectedRefreshToken = "refreshToken";
        when(service.refreshToken(request))
                .thenReturn(new AuthResponse(expectedAccessToken, expectedRefreshToken));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        AuthResponse authResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(result).isNotNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isEqualTo(expectedAccessToken);
        assertThat(authResponse.refreshToken()).isEqualTo(expectedRefreshToken);
    }
}
