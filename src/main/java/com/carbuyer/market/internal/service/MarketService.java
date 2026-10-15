package com.carbuyer.market.internal.service;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.NotCarOwnerException;
import com.carbuyer.exception.exceptions.NotListingOwnerException;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.api.dto.MarketListingResponseDto;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import com.carbuyer.market.internal.repository.MarketListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketListingRepository repository;
    private final CarFacade carFacade;

    @Transactional
    public MarketListingResponseDto createListing(Long sellerId, CreateListingRequestDto request) {
        UUID carId = request.carId();
        if (!carFacade.isCarOwner(carId, sellerId)) {
            throw new NotCarOwnerException("This seller: " + sellerId + " is not owner");
        }
        BigDecimal price = request.price();
        MarketListing listing = new MarketListing(carId, sellerId, null, price);
        MarketListing savedListing = repository.save(listing);
        return new MarketListingResponseDto(
                savedListing.getId(),
                savedListing.getCarId(),
                savedListing.getSellerId(),
                savedListing.getPrice(),
                savedListing.getStatus().toString(),
                savedListing.getCreatedAt());
    }

    @Transactional
    public void cancelListing(Long sellerId, UUID listingId) {
        MarketListing existListing = repository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing with id: " + listingId + " not found"));
        if (!existListing.getSellerId().equals(sellerId)) {
            throw new NotListingOwnerException("This listing does not belong to: " + sellerId);
        }
        existListing.setStatus(ListingStatus.CANCELLED);
        existListing.setClosedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public Page<MarketListingResponseDto> findAllByStatus(Pageable pageable) {
        return repository.findAllByStatus(ListingStatus.ACTIVE, pageable)
                .map(listing -> new MarketListingResponseDto(
                listing.getId(),
                listing.getCarId(),
                listing.getSellerId(),
                listing.getPrice(),
                listing.getStatus().toString(),
                listing.getCreatedAt()));
    }
}
