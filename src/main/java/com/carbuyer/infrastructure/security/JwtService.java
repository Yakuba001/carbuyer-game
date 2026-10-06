package com.carbuyer.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.security.jwt.secret}")
    private String secretKey;
    @Value("${app.security.jwt.access-token-ttl-ms}")
    private long accessTokenTtl;
    @Value("${app.security.jwt.refresh-token-ttl-ms}")
    private long refreshTokenTtl;
    private SecretKey signingKey;
    private JwtParser jwtParser;

    @PostConstruct
    private void init() {
        signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        jwtParser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String generateAccessToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenTtl))
                .signWith(signingKey).compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenTtl))
                .signWith(signingKey).compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String usernameFromToken = extractUsername(token);
        return usernameFromToken.equals(userDetails.getUsername());
    }

    private Claims extractAllClaims(String token) {
        return jwtParser
                .parseSignedClaims(token)
                .getPayload();
    }
}
