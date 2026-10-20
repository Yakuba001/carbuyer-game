package com.carbuyer.car.internal.service;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.repository.CarRepository;
import com.carbuyer.exception.exceptions.CarNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarService {

    private final CarRepository carRepository;

    public boolean isCarOwner(UUID carId, Long sellerId) {
        Car car = carRepository.findForUpdateById(carId)
                .orElseThrow(() -> new CarNotFoundException("Car with this id: " + carId + " does not exists"));
        Long ownerId = car.getOwnerId();
        if (sellerId == null || ownerId == null) {
            return false;
        }
        return sellerId.equals(ownerId);
    }

    @Transactional
    public boolean changeCarOwner(Long ownerId, Long newOwnerId, UUID carId) {
        int rowsChanged = carRepository.setNewCarOwner(ownerId, newOwnerId, carId);
        return rowsChanged != 0;
    }
}
