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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GarageService {

    private final GarageSlotRepository repository;

    public Long getGarageSlotIdWithEmptyStatus(Long userId, String status) {
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
}
