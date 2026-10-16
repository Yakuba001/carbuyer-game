package com.carbuyer.garage.internal;

import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.garage.internal.service.GarageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
class GarageFacadeImpl implements GarageFacade {

    private final GarageService service;

    @Override
    public boolean isHasGarageSlotWithInputStatus(Long userId, String status) {
        return service.isHasGarageSlotWithInputStatus(userId, status);
    }

    @Override
    public boolean isChangedStatusFromEmptyToOccupiedAndSetCarId(Long userId, UUID carId, String status) {
        return service.isChangedStatusFromEmptyToOccupiedAndSetCarId(userId, carId, status);
    }
}
