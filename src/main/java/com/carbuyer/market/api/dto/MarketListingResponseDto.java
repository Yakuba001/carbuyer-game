package com.carbuyer.market.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketListingResponseDto(
        @Schema(description = "Listing id", example = "aeb1430d-f3f9-4bde-a7f1-5abccec356b3")
        UUID id,
        @Schema(description = "Car id", example = "aeb1430d-f3f9-4bde-a7f1-5abccec356b2")
        UUID carId,
        @Schema(description = "Seller id", example = "1")
        Long sellerId,
        @Schema(description = "Listing price", type = "number", format = "double", example = "0.00")
        BigDecimal price,
        @Schema(description = "Listing status", example = "ACTIVE")
        String status,
        @Schema(description = "Created at for listing", example = "2026-10-26T14:15:00Z")
        Instant createdAt
) {
}
