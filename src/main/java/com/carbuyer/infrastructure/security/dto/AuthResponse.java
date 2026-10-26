package com.carbuyer.infrastructure.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT Access valid for a short period.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access...")
        String accessToken,
        @Schema(description = "JWT Refresh valid for a long period.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...")
        String refreshToken
) {
}
