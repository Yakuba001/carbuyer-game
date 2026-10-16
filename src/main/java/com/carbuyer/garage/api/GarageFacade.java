package com.carbuyer.garage.api;

import java.util.UUID;

public interface GarageFacade {

    boolean isHasGarageSlotWithInputStatus(Long userId, String status);
    boolean isChangedStatusFromEmptyToOccupiedAndSetCarId(Long userId, UUID carId, String status);
}
