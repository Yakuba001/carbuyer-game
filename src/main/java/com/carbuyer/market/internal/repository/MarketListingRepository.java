package com.carbuyer.market.internal.repository;

import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MarketListingRepository extends JpaRepository<MarketListing, UUID> {

    Page<MarketListing> findAllByStatus(ListingStatus status, Pageable pageable);
}
