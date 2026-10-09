package com.carbuyer.car.internal.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "cars")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Car {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Setter
    @Column(name = "vin", length = 17, unique = true, nullable = false)
    private String vin;
    @Setter
    @Column(name = "model", length = 64, nullable = false)
    private String model;
    @Setter
    @Column(name = "mileage", nullable = false)
    private Integer mileage = 0;
    @Setter
    @Column(name = "condition_score", nullable = false)
    private Integer conditionScore;
    @Setter
    @Column(name = "real_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal realPrice = new BigDecimal("0.00");
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Car(String vin, String model, Integer mileage, Integer conditionScore, BigDecimal realPrice, Long ownerId) {
        this.vin = vin;
        this.model = model;
        this.mileage = mileage;
        this.conditionScore = conditionScore;
        this.realPrice = realPrice;
        this.ownerId = ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Car that)) return false;
        if (this.getId() == null || that.getId() == null) return false;
        return this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
