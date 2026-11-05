package com.carbuyer.car.internal.service;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.entity.CarModels;
import com.carbuyer.car.internal.repository.CarRepository;
import com.carbuyer.exception.exceptions.CarNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    @Transactional
    public List<UUID> addCarsAndSetNewOwners(List<Long> ownerIds) {
        List<Car> cars = ownerIds.stream()
                .map(ownerId -> new Car(
                        vinGenerator(),
                        modelGenerator(),
                        mileageGenerator(),
                        conditionScoreGenerator(),
                        realPriceGenerator(),
                        ownerId))
                .toList();
        List<Car> savedCars = carRepository.saveAllAndFlush(cars);
        return savedCars.stream()
                .map(Car::getId)
                .toList();
    }

    private String vinGenerator() {
        return RandomStringUtils.randomAlphanumeric(17);
    }

    private String modelGenerator() {
        CarModels[] allModels = CarModels.values();
        return allModels[ThreadLocalRandom.current().nextInt(0, allModels.length)].name();
    }

    private int mileageGenerator() {
        return ThreadLocalRandom.current().nextInt(1, 300000);
    }

    private int conditionScoreGenerator() {
        return ThreadLocalRandom.current().nextInt(1, 101);
    }

    private BigDecimal realPriceGenerator() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0, 10000));
    }
}
