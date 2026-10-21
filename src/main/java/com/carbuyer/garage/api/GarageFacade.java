package com.carbuyer.garage.api;

import java.util.UUID;

public interface GarageFacade {

    Long getGarageSlotIdWithEmptyStatus(Long userId, String status);

    Long getSellerGarageSlotId(Long userId, UUID carId);

    void changeGarageSlotStatusesAndIdInIt(Long emptyGarageSlotId,
                                           Long sellerGarageSlotId,
                                           UUID carId);
}
