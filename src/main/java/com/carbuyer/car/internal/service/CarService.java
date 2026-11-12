package com.carbuyer.car.internal.service;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.entity.CarModels;
import com.carbuyer.car.internal.repository.CarRepository;
import com.carbuyer.exception.exceptions.CarNotFoundException;
import com.carbuyer.exception.exceptions.EmptyInputCollectionException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarService {

    private final CarRepository carRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

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
        MapSqlParameterSource[] carParams = ownerIds.stream()
                .map(ownerId -> {
                    MapSqlParameterSource param = new MapSqlParameterSource();
                    param.addValue("vin", vinGenerator());
                    param.addValue("model", modelGenerator());
                    param.addValue("mileage", mileageGenerator());
                    param.addValue("condition_score", conditionScoreGenerator());
                    param.addValue("owner_id", ownerId);
                    return param;
                }).toArray(MapSqlParameterSource[]::new);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO cars (vin, model, mileage, condition_score, owner_id) " +
                "VALUES (:vin, :model, :mileage, :condition_score, :owner_id)";
        jdbcTemplate.batchUpdate(sql, carParams, keyHolder, new String[]{"id"});
        return keyHolder.getKeyList().stream()
                .map(map -> ((UUID) map.get("id"))).toList();
    }

    @Transactional
    public void deleteCarsByOwnerIds(List<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) throw new EmptyInputCollectionException("Owner ids cannot be empty");
        carRepository.deleteByOwnerIdIn(ownerIds);
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
}
