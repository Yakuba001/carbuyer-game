package com.carbuyer.market.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MarketListingResponseDto(
        UUID id,
        UUID carId,
        Long sellerId,
        BigDecimal price,
        String status,
        Instant createdAt
) {
}
