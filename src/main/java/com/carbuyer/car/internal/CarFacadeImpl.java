package com.carbuyer.car.internal;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.car.internal.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarFacadeImpl implements CarFacade {

    private final CarService service;

    @Override
    public boolean isCarOwner(UUID carId, Long sellerId) {
        return service.isCarOwner(carId, sellerId);
    }

    @Override
    public boolean setNewCarOwner(Long sellerId, Long buyerId, UUID carId) {
        return service.changeCarOwner(sellerId, buyerId, carId);
    }
}
