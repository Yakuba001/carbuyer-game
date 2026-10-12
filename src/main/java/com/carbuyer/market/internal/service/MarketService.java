package com.carbuyer.market.internal.service;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import com.carbuyer.market.internal.repository.MarketListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketListingRepository repository;
    private final CarFacade carFacade;

    @Transactional
    public UUID createListing(Long sellerId, CreateListingRequestDto request) {
        UUID carId = request.carId();
        if (!carFacade.isCarOwner(carId, sellerId)) {
            throw new IllegalArgumentException("This seller is not owner of car");
        }
        BigDecimal price = request.price();
        MarketListing listing = new MarketListing(carId, sellerId, null, price);
        MarketListing savedListing = repository.save(listing);
        return savedListing.getId();
    }

    @Transactional
    public void cancelListing(Long sellerId, UUID listingId) {
        MarketListing existListing = repository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing with id: " + listingId + " not found"));
        if (!existListing.getSellerId().equals(sellerId)) {
            throw new RuntimeException("This listing does not belong to: " + sellerId);
        }
        existListing.setStatus(ListingStatus.CANCELLED);
    }
}
