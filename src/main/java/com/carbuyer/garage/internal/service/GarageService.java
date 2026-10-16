package com.carbuyer.garage.internal.service;

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

    public boolean isHasGarageSlotWithInputStatus(Long userId, String status) {
        if (status == null) {
            return false;
        }
        try {
            GarageSlotStatus enumStatus = GarageSlotStatus.valueOf(status.trim().toUpperCase());
            return repository.existsByUserIdAndStatus(userId, enumStatus);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional
    public boolean isChangedStatusFromEmptyToOccupiedAndSetCarId(Long userId, UUID carId, String status) {
        if (status == null) {
            return false;
        }
        try {
            GarageSlotStatus enumStatus = GarageSlotStatus.valueOf(status.trim().toUpperCase());
            int result = repository.occupyFirstEmptySlot(userId, carId, enumStatus);
            return result != 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
