package com.carbuyer.garage.internal.service;

import com.carbuyer.exception.exceptions.GarageSlotNotFoundException;
import com.carbuyer.exception.exceptions.NoEmptySlotsException;
import com.carbuyer.exception.exceptions.NotCarOwnerException;
import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GarageService {

    private final GarageSlotRepository repository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Long getGarageSlotIdByUserIdAndStatus(Long userId, String status) {
        GarageSlotStatus enumStatus = GarageSlotStatus.valueOf(status.trim().toUpperCase());
        GarageSlot emptyGarageSlot = repository.findFirstByUserIdAndStatus(userId, enumStatus)
                .orElseThrow(() -> new NoEmptySlotsException("You don`t have empty slots"));
        return emptyGarageSlot.getId();
    }

    public Long getSellerGarageSlotId(Long userId, UUID carId) {
        GarageSlot sellerGarageSlot = repository.findByUserIdAndCarId(userId, carId)
                .orElseThrow(() -> new NotCarOwnerException("You don`t have seller slots"));
        return sellerGarageSlot.getId();
    }

    @Transactional
    public void garageSlotStatusesAndCarIdInItChanger(Long emptyGarageSlotId,
                                                      Long sellerGarageSlotId,
                                                      UUID carId) {
        GarageSlot buyerGarageSlot = repository.findById(emptyGarageSlotId)
                .orElseThrow(() ->
                        new GarageSlotNotFoundException("Garage slot with id " + emptyGarageSlotId + " not found"));
        GarageSlot sellerGarageSlot = repository.findById(sellerGarageSlotId)
                .orElseThrow(() ->
                        new GarageSlotNotFoundException("Garage slot with id " + sellerGarageSlotId + " not found"));
        sellerGarageSlot.setCarId(null);
        sellerGarageSlot.setStatus(GarageSlotStatus.EMPTY);
        repository.saveAndFlush(sellerGarageSlot);
        buyerGarageSlot.setCarId(carId);
        buyerGarageSlot.setStatus(GarageSlotStatus.OCCUPIED);
    }

    @Transactional
    public void changeGarageSlotStatusAndSetCarInItByCarIds(List<Long> userIds, List<UUID> carIds) {
        if (userIds.size() != carIds.size()) {
            throw new GarageSlotNotFoundException("Math error: lists size mismatch");
        }
        MapSqlParameterSource[] slotParams = new MapSqlParameterSource[userIds.size()];
        for (int i = 0; i < userIds.size(); i++) {
            MapSqlParameterSource param = new MapSqlParameterSource();
            param.addValue("userId", userIds.get(i));
            param.addValue("carId", carIds.get(i));
            param.addValue("status", GarageSlotStatus.OCCUPIED.name());
            slotParams[i] = param;
        }
        String sql = "UPDATE garage_slots SET car_id = :carId, status = :status " +
                "WHERE user_id = :userId AND status = 'EMPTY'";
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, slotParams);
        int totalUpdated = 0;
        for (int el : updateCounts) {
            totalUpdated += el;
        }
        if (totalUpdated != userIds.size()) {
            throw new GarageSlotNotFoundException("Not enough empty slots found for update");
        }
    }

    @Transactional
    public void addGarageSlot(Long userId, int slotNumber) {
        GarageSlot slot = new GarageSlot(userId, slotNumber, null, GarageSlotStatus.EMPTY);
        repository.save(slot);
    }

    @Transactional
    public void addGarageSlots(List<Long> userIds) {
        int defaultSlotNumber = 1;
        MapSqlParameterSource[] garageSlotParams = userIds.stream()
                .map(userId -> {
                    MapSqlParameterSource param = new MapSqlParameterSource();
                    param.addValue("user_id", userId);
                    param.addValue("slot_number", defaultSlotNumber);
                    param.addValue("car_id", null);
                    return param;
                })
                .toArray(MapSqlParameterSource[]::new);
        String sql = "INSERT INTO garage_slots (user_id, slot_number, car_id) VALUES (:user_id, :slot_number, :car_id)";
        jdbcTemplate.batchUpdate(sql, garageSlotParams);
    }
}
