package com.carbuyer.market.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface MarketFacade {

    void createBotListing(Long sellerId, UUID carId, BigDecimal price);
    void createListings(List<Long> sellerIds, List<UUID> carIds);
    UUID getRandomListingId();
    void buyCar(Long buyerId, UUID listingId);
    Long getBotSellerId(UUID listingId);
    UUID getListingCarId(UUID listingId);
    void deleteListingsBySellerIds(List<Long> sellerIds);
}
