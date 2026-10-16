package com.carbuyer.garage.internal;

import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.garage.internal.service.GarageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class GarageFacadeImpl implements GarageFacade {

    private final GarageService service;

    @Override
    public boolean isHasGarageSlotWithInputStatus(Long userId, String status) {
        return service.isHasGarageSlotWithInputStatus(userId, status);
    }
}
