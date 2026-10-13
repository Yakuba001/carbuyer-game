package com.carbuyer.market.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequestDto(
        @NotNull UUID carId,
        @NotNull @Positive BigDecimal price
) { }
