package com.carbuyer.market.internal.service;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.*;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.api.dto.MarketListingResponseDto;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import com.carbuyer.market.internal.repository.MarketListingRepository;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketService {

    private final MarketListingRepository repository;
    private final CarFacade carFacade;
    private final UserFacade userFacade;
    private final GarageFacade garageFacade;

    private static final String EMPTY_GARAGE_SLOT = "EMPTY";
    private static final String OCCUPIED_GARAGE_SLOT = "OCCUPIED";

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

    @Transactional
    public void buyCar(Long buyerId, UUID listingId) {
        if (listingId == null || buyerId == null) {
            throw new MissedDataException("ListingId and userId are required: " + listingId + ", " + buyerId);
        }
        if (!userFacade.existsById(buyerId)) {
            throw new UserNotFoundException("User with id: " + buyerId + " not found");
        }
        MarketListing listing = repository.findByIdForUpdate(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing with id: " + listingId + " not found"));
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new InactiveListingException("Listing with id: " + listingId + " is not active");
        }
        Long sellerId = listing.getSellerId();
        Long firstIdForBlocking = Math.min(buyerId, sellerId);
        Long secondIdForBlocking = Math.max(buyerId, sellerId);
        BigDecimal buyerBalance, sellerBalance;
        if (firstIdForBlocking.equals(buyerId)) {
            buyerBalance = userFacade.getUserBalanceById(firstIdForBlocking);
            sellerBalance = userFacade.getUserBalanceById(secondIdForBlocking);
        } else {
            sellerBalance = userFacade.getUserBalanceById(firstIdForBlocking);
            buyerBalance = userFacade.getUserBalanceById(secondIdForBlocking);
        }
        if (buyerBalance.compareTo(listing.getPrice()) < 0) {
            throw new InsufficientFundsException("You don`t have enough funds");
        }
        if (Objects.equals(sellerId, buyerId)) {
            throw new SelfBuyingException("You can`t buy your own car");
        }
        if (!garageFacade.isHasGarageSlotWithInputStatus(buyerId, EMPTY_GARAGE_SLOT)) {
            throw new NoEmptySlotsException("You don`t have empty slots");
        }
        if (!userFacade.carPurchase(buyerId, sellerId, buyerBalance, sellerBalance, listing.getPrice())) {
            throw new InsufficientFundsException("Something went wrong");
        }


        // TODO: THIS

        if (!carFacade.setNewCarOwner(sellerId, buyerId, listing.getCarId())) {
            throw new ChangeCarOwnerException("Something went wrong");
        }
        if (!garageFacade.isChangedStatusFromEmptyToOccupiedAndSetCarId(
                buyerId,
                listing.getCarId(),
                OCCUPIED_GARAGE_SLOT)) {
            throw new ChangeCarOwnerException("Something went wrong");
        }
        listing.setStatus(ListingStatus.SOLD);
        listing.setBuyerId(buyerId);
        listing.setClosedAt(Instant.now());
    }
}
