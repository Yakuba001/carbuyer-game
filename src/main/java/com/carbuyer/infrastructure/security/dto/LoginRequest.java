package com.carbuyer.infrastructure.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email
        @Schema(description = "User`s registered email address.", example = "Boss")
        String email,
        @NotBlank
        @Schema(description = "User`s account password.", example = "boss@gmail.com")
        String password
) {
}
