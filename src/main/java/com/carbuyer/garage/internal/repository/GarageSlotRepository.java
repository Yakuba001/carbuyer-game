package com.carbuyer.garage.internal.repository;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GarageSlotRepository extends JpaRepository<GarageSlot, Long> {

    List<GarageSlot> findByUserId(Long userId);
    Optional<GarageSlot> findByUserIdAndSlotNumber(Long userId, Integer slotNumber);
    long countByUserIdAndStatus(Long userId, GarageSlotStatus status);
}
