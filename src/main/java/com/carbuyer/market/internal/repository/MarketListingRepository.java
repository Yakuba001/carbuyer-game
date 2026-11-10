package com.carbuyer.market.internal.repository;

import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MarketListingRepository extends JpaRepository<MarketListing, UUID> {

    Page<MarketListing> findAllByStatus(ListingStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MarketListing m WHERE m.id = :id")
    Optional<MarketListing> findByIdForUpdate(@Param("id") UUID id);
    @Query(value = """
    SELECT l.id FROM market_listings l WHERE l.status = 'ACTIVE' ORDER BY random() LIMIT 1
    """, nativeQuery = true)
    Optional<UUID> findRandomActiveListingId();
    @Query(value = """
    SELECT l.seller_id FROM market_listings l JOIN users u ON l.seller_id = u.id WHERE l.id = :id AND u.is_bot = true
    """, nativeQuery = true)
    Optional<Long> findBotSellerId(@Param("id") UUID id);
}
