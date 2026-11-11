package com.carbuyer.market.internal;

import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.internal.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class MarketFacadeImpl implements MarketFacade {

    private final MarketService marketService;

    @Override
    public void createListings(List<Long> sellerIds, List<UUID> carIds) {
        marketService.createListings(sellerIds, carIds);
    }

    @Override
    public UUID getRandomListingId() {
        return marketService.getRandomActiveListingId();
    }

    @Override
    public void buyCar(Long buyerId, UUID listingId) {
        marketService.buyCar(buyerId, listingId);
    }

    @Override
    public Long getBotSellerId(UUID listingId) {
        return marketService.getBotSellerId(listingId);
    }

    @Override
    public void createBotListing(Long sellerId, UUID carId, BigDecimal price) {
        CreateListingRequestDto requestDto = new CreateListingRequestDto(carId, price);
        marketService.createListing(sellerId, requestDto);
    }

    @Override
    public UUID getListingCarId(UUID listingId) {
        return marketService.getListingCarId(listingId);
    }
}
