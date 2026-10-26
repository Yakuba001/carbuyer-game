package com.carbuyer.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ErrorResponse(
        @Schema(description = "HTTP Status Code", example = "404")
        int status,
        @Schema(description = "Error title", example = "Not Found")
        String error,
        @Schema(description = "Detailed error message from the exception", example = "User not found with id: 5")
        String message,
        @Schema(description = "Timestamp of the error")
        Instant timestamp
) {
}
