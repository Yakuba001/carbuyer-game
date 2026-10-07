package com.carbuyer.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

    @Mock
    private UserDetails userDetails;
    @InjectMocks
    private JwtService jwtService;

    private final String testSecretKey = "8a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t1u2v3w4x5y6z7A8B9C0D1E2F";
    private final long testAccessTokenTtl = 60000;
    private final long testRefreshTokenTtl = 10000;
    private final String expectedUser = "testUser";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "accessTokenTtl", testAccessTokenTtl);
        ReflectionTestUtils.setField(jwtService, "refreshTokenTtl", testRefreshTokenTtl);

        ReflectionTestUtils.invokeMethod(jwtService, "init");
    }


    @Test
    void generateAccessToken_withCorrectTtl_thenReturnAccessTokenWithCorrectExpiration() {
        when(userDetails.getUsername()).thenReturn(expectedUser);
        String result = jwtService.generateAccessToken(userDetails);

        assertThat(result).isNotNull();

        Claims payload = getPayload(result);

        String subject = payload.getSubject();
        Date issuedAt = payload.getIssuedAt();
        Date actualExpiration = payload.getExpiration();
        Date expectedExpiration = new Date(issuedAt.getTime() + testAccessTokenTtl);

        assertThat(actualExpiration).isCloseTo(expectedExpiration, 1000);
        assertThat(subject).isEqualTo(expectedUser);
    }

    @Test
    void generateRefreshToken_withCorrectRefreshToken_thenReturnRefreshTokenWithCorrectExpiration() {
        when(userDetails.getUsername()).thenReturn(expectedUser);

        String result = jwtService.generateRefreshToken(userDetails);

        assertThat(result).isNotNull();

        Claims payload = getPayload(result);
        Date issuedAt = payload.getIssuedAt();
        Date actualExpiration = payload.getExpiration();
        Date expectedExpiration = new Date(issuedAt.getTime() + testRefreshTokenTtl);

        assertThat(actualExpiration).isCloseTo(expectedExpiration, 1000);
    }

    @Test
    void extractUsername_returnsUsername() {
        String token = generateTestToken(expectedUser, testAccessTokenTtl);

        String result = jwtService.extractUsername(token);

        assertThat(result).isEqualTo(expectedUser);
    }

    @Test
    void isTokenValid_whenUserMatch_returnsTrue() {
        String token = generateTestToken(expectedUser, testAccessTokenTtl);
        when(userDetails.getUsername()).thenReturn(expectedUser);

        boolean isValid = jwtService.isTokenValid(token,  userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_whenUserDoesNotMatch_returnsFalse() {
        String username = "invalidUser";
        String token = generateTestToken(username, testAccessTokenTtl);
        when(userDetails.getUsername()).thenReturn(expectedUser);

        boolean isTokenValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void isTokenValid_whenTokenExpired_shouldThrowException() {
        String token = generateTestToken(expectedUser, -1);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    private String generateTestToken(String username, long ttlMs) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private Claims getPayload(String jwt) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
