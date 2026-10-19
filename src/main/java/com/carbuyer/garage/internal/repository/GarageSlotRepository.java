package com.carbuyer.garage.internal.repository;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GarageSlotRepository extends JpaRepository<GarageSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GarageSlot> findFirstByUserIdAndStatus(Long userId, GarageSlotStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE GarageSlot g SET g.carId = :carId, g.status = :newStatus " +
            "WHERE g.id = (SELECT sub.id FROM GarageSlot sub " +
            "WHERE sub.userId = :userId AND sub.status = 'EMPTY' " +
            "ORDER BY sub.slotNumber ASC LIMIT 1)")
    int occupyFirstEmptySlot(@Param("userId") Long userId,
                             @Param("carId") UUID carId,
                             @Param("newStatus") GarageSlotStatus status);
}
