package com.carbuyer.market.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequestDto(
        UUID carId,
        BigDecimal price
) { }
