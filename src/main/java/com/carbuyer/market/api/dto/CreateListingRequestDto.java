package com.carbuyer.market.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateListingRequestDto(
        @NotNull
        @Schema(description = "Unique car id for listing", example = "aeb1430d-f3f9-4bde-a7f1-5abccec356b3")
        UUID carId,
        @Schema(description = "Positive price for listing", type = "number", format = "double", example = "0.00")
        @NotNull @Positive BigDecimal price
) { }
