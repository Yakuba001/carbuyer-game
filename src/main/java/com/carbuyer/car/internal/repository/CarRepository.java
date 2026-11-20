package com.carbuyer.car.internal.repository;

import com.carbuyer.car.internal.entity.Car;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
    DELETE FROM cars c WHERE c.owner_id IN (:ownerIds)
    AND EXISTS (SELECT 1 FROM users u WHERE u.id = c.owner_id AND u.is_bot = true)
    AND NOT EXISTS (SELECT 1 FROM market_listings l WHERE l.car_id = c.id)
    """, nativeQuery = true)
    void deleteByOwnerIdIn(@Param("ownerIds") List<Long> ownerIds);
}
