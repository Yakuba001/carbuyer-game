package com.carbuyer.infrastructure.security.controller;

import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.infrastructure.security.dto.AuthResponse;
import com.carbuyer.infrastructure.security.dto.LoginRequest;
import com.carbuyer.infrastructure.security.dto.RefreshTokenRequest;
import com.carbuyer.infrastructure.security.dto.RegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthControllerTest extends BaseIntegrationTest {

    private static final String TEST_USERNAME = "Boss";
    private static final String TEST_EMAIL = "boss@gmail.com";
    private static final String TEST_PASSWORD = "12345678";

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @AfterEach
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void register_shouldReturn201_whenUserWithThisNameOrEmailDoesNotExistAndRequestWithValidData() {
        RegisterRequest registerRequest = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse authResponse = response.getBody();
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotNull();
        assertThat(authResponse.refreshToken()).isNotNull();
    }

    @Test
    void register_shouldReturn400_whenFieldsInRequestInvalid() {
        RegisterRequest registerRequest = new RegisterRequest(TEST_USERNAME, "Invalid", TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_shouldReturn200_whenUserExistsAndLoginPasswordIsCorrect() {
        registerUserAndGetHisRefreshToken();
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, AuthResponse.class);
        AuthResponse authResponse = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotNull();
        assertThat(authResponse.refreshToken()).isNotNull();
    }

    @Test
    void login_shouldReturn400_whenFieldsInRequestInvalid() {
        LoginRequest loginRequest = new LoginRequest("Invalid", TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_shouldReturn401_whenUserWithThisCredentialsDoesNotExist() {
        LoginRequest loginRequest = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_shouldReturn200_whenUserExistsAndRefreshTokenIsCorrect() {
        String exampleToken = registerUserAndGetHisRefreshToken();
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(exampleToken);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/refresh", refreshTokenRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponse authResponse = response.getBody();

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotNull();
        assertThat(authResponse.refreshToken()).isNotNull();
        assertThat(authResponse.refreshToken()).isEqualTo(exampleToken);
    }

    @Test
    void refresh_shouldReturn400_whenFieldsInRequestInvalid() {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("");

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/refresh", refreshTokenRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String registerUserAndGetHisRefreshToken() {
        RegisterRequest registerRequest = new RegisterRequest(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);
        AuthResponse authResponse = response.getBody();

        assertThat(authResponse).isNotNull();
        return authResponse.refreshToken();
    }

    @Test
    void forbiddenAccess_shouldReturn403_whenSomebodyTryVisitLinkHeIsAnonymous() {
        ResponseEntity<AuthResponse> response = restTemplate
                .getForEntity("/api/v1/market", AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
