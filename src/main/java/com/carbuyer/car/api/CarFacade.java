package com.carbuyer.car.api;

import java.util.UUID;

public interface CarFacade {

    boolean isCarOwner(UUID carId, Long sellerId);
    boolean setNewCarOwner(Long sellerId, Long buyerId, UUID carId);
}
