package com.carbuyer.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StartBotResponse(
        @Schema(description = "Bots status", example = "Bots enabled")
        String status,
        @Schema(description = "Amount of bots", example = "100")
        int amount
) {
}
