package com.carbuyer.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record StartBotRequest(
        @NotNull
        @Positive
        @Min(1)
        @Max(999)
        @Schema(description = "Start bot amount with borders from 1 to 999", example = "100")
        int amount,
        @NotBlank
        @Size(min = 8, max = 255)
        @Schema(description = "Set example password for future bots", example = "12345678")
        String botPassword
) {
}
