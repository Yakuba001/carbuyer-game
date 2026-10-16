package com.carbuyer.garage.internal.repository;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarageSlotRepository extends JpaRepository<GarageSlot, Long> {

    boolean existsByUserIdAndStatus(Long userId, GarageSlotStatus status);
}
