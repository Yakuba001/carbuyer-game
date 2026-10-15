package com.carbuyer.car.internal.service;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.repository.CarRepository;
import com.carbuyer.exception.exceptions.CarNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    public boolean isCarOwner(UUID carId, Long sellerId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new CarNotFoundException("Car with this id: " + carId + " does not exists"));
        Long ownerId = car.getOwnerId();
        if (sellerId == null || ownerId == null) {
            return false;
        }
        return sellerId.equals(ownerId);
    }
}
