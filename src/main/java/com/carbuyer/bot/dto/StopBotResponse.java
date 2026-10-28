package com.carbuyer.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StopBotResponse(
        @Schema(description = "Bots status", example = "Bots disabled")
        String status
) {
}
