package com.carbuyer.infrastructure.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank
        @Schema(description = "Registration time JWT during login/registration.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...")
        String refreshToken
) {
}
