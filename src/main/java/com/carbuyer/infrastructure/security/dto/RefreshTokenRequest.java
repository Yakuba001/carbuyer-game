package com.carbuyer.infrastructure.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank
        @Schema(description = "Registration time JWT during login/registration.",
                example = "8a2b3c4d5e6f7g8h9i0j1k2l3m4n5o6p7q8...refresh")
        String refreshToken
) {
}
