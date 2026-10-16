package com.carbuyer.garage.internal.service;

import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
}
