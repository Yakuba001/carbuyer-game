package com.carbuyer.car.internal.repository;

import com.carbuyer.car.internal.entity.Car;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CarRepository extends JpaRepository<Car, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Car c SET c.ownerId = :newOwnerId " +
            "WHERE c.id = :carId AND c.ownerId = :ownerId")
    int setNewCarOwner(@Param("ownerId") Long ownerId,
                       @Param("newOwnerId") Long newOwnerId,
                       @Param("carId") UUID carId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.id = :carId")
    Optional<Car> findForUpdateById(@Param("carId") UUID carId);
}
