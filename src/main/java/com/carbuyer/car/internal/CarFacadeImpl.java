package com.carbuyer.car.internal;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarFacadeImpl implements CarFacade {

    private final CarRepository carRepository;

    @Override
    public boolean isCarOwner(UUID carId, Long sellerId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car with this id does not exists"));
        Long ownerId = car.getOwnerId();
        if (sellerId == null || ownerId == null) {
            return false;
        }
        return sellerId.equals(ownerId);
    }
}
