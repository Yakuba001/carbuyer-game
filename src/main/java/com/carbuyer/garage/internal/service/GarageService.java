package com.carbuyer.garage.internal.service;

import com.carbuyer.exception.exceptions.GarageSlotNotFoundException;
import com.carbuyer.exception.exceptions.NoEmptySlotsException;
import com.carbuyer.exception.exceptions.NotCarOwnerException;
import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GarageService {

    private final GarageSlotRepository repository;

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
        buyerGarageSlot.setCarId(carId);
        buyerGarageSlot.setStatus(GarageSlotStatus.OCCUPIED);
        sellerGarageSlot.setCarId(null);
        sellerGarageSlot.setStatus(GarageSlotStatus.EMPTY);
    }

    @Transactional
    public void changeGarageSlotStatusAndSetCarInItByCarIds(List<Long> userIds, List<UUID> carIds) {
        List<GarageSlot> emptySlots = repository.findEmptySlotsForUpdate(userIds);
        if (emptySlots.size() == userIds.size() && emptySlots.size() == carIds.size()) {
            for (int i = 0; i < emptySlots.size(); i++) {
                GarageSlot currSlot = emptySlots.get(i);
                currSlot.setCarId(carIds.get(i));
                currSlot.setStatus(GarageSlotStatus.OCCUPIED);
            }
        } else throw new GarageSlotNotFoundException("Math error not enough empty slots");
    }

    @Transactional
    public void addGarageSlot(Long userId, int slotNumber) {
        GarageSlot slot = new GarageSlot(userId, slotNumber, null, GarageSlotStatus.EMPTY);
        repository.save(slot);
    }

    @Transactional
    public void addGarageSlots(List<Long> userIds) {
        int defaultSlotNumber = 1;
        List<GarageSlot> garageSlots = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            GarageSlot slot = new GarageSlot(userId, defaultSlotNumber, null, GarageSlotStatus.EMPTY);
            garageSlots.add(slot);
        }
        repository.saveAllAndFlush(garageSlots);
    }
}
