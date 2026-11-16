package com.carbuyer.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StartBotResponse(
        @Schema(description = "Bots status", example = "Bots successfully started")
        String status,
        @Schema(description = "Number of worker threads", example = "Worker count: 10")
        int numberOfBots
) {
}
