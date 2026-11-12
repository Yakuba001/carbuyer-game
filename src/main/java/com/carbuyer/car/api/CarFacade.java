package com.carbuyer.car.api;

import java.util.List;
import java.util.UUID;

public interface CarFacade {

    boolean isCarOwner(UUID carId, Long sellerId);
    boolean setNewCarOwner(Long sellerId, Long buyerId, UUID carId);
    List<UUID> addCarsByIds(List<Long> ownerIds);
    void deleteCarsByOwnerIds(List<Long> ownerIds);
}
