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
    public Long getGarageSlotIdWithEmptyStatus(Long userId, String status) {
        return service.getGarageSlotIdWithEmptyStatus(userId, status);
    }

    @Override
    public Long getSellerGarageSlotId(Long userId, UUID carId) {
        return service.getSellerGarageSlotId(userId, carId);
    }

    @Override
    public void changeGarageSlotStatusesAndIdInIt(Long emptyGarageSlotId,
                                                  Long sellerGarageSlotId,
                                                  UUID carId) {
        service.garageSlotStatusesAndCarIdInItChanger(
                emptyGarageSlotId,
                sellerGarageSlotId,
                carId);
    }

}
