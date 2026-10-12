package com.carbuyer.market.internal.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketListing {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Setter
    @Column(name = "car_id", nullable = false)
    private UUID carId;
    @Setter
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    @Setter
    @Column(name = "buyer_id")
    private Long buyerId;
    @Setter
    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal price = new BigDecimal("0.00");
    @Setter
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Setter
    @Column(name = "closed_at")
    private Instant closedAt;

    public MarketListing(UUID carId, Long sellerId, Long buyerId, BigDecimal price) {
        this.carId = carId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.price = price;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MarketListing that)) return false;
        if (this.getId() == null || that.getId() == null) return false;
        return this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
