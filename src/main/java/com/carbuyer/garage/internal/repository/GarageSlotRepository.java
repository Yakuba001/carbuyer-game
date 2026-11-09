package com.carbuyer.garage.internal.repository;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface GarageSlotRepository extends JpaRepository<GarageSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GarageSlot> findFirstByUserIdAndStatus(Long userId, GarageSlotStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GarageSlot> findByUserIdAndCarId(Long userId, UUID carId);
}
