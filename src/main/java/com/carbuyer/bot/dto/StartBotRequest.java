package com.carbuyer.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartBotRequest(
        @NotNull
        @Positive
        @Min(1)
        @Max(999)
        @Schema(description = "Start bot amount with borders from 1 to 999", example = "100")
        int amount
) {
}
