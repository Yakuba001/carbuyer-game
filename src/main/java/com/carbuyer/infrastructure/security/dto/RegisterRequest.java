package com.carbuyer.infrastructure.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Schema(description = "Unique username for the new account.", example = "Boss")
        String username,
        @NotBlank @Email
        @Schema(description = "Unique email for the new account.", example = "boss@gmail.com")
        String email,
        @NotBlank @Size(min = 8)
        @Schema(description = "Password for the new account, must be at least 8 chars long.", example = "12345678")
        String password
) {
}
