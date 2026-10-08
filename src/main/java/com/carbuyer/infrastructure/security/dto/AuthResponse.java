package com.carbuyer.infrastructure.security.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {
}
