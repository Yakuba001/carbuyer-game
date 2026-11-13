package com.carbuyer.bot.dto;

import java.util.UUID;

public record PurchaseResult(UUID listingId, UUID carId, Long sellerId) {
}
