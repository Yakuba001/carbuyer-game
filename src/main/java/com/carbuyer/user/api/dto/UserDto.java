package com.carbuyer.user.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record UserDto(
        Long id,
        String name,
        String email,
        BigDecimal balance,
        Integer maxSlots,
        Instant createdAt
) {
}
