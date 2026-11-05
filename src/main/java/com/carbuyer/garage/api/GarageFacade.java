package com.carbuyer.garage.api;

import java.util.List;
import java.util.UUID;

public interface GarageFacade {

    Long getGarageSlotIdByUserIdAndStatus(Long userId, String status);
    Long getSellerGarageSlotId(Long userId, UUID carId);
    void changeGarageSlotStatusesAndIdInIt(Long emptyGarageSlotId,
                                           Long sellerGarageSlotId,
                                           UUID carId);
    void changeEmptyGarageSlotStatusAndSetInItCarId(List<Long> userIds, List<UUID> carIds);
    void addGarageSlot(Long userId, int slotNumber);
    void addGarageSlots(List<Long> userIds);
}
