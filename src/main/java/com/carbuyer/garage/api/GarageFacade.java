package com.carbuyer.garage.api;

public interface GarageFacade {

    boolean isHasGarageSlotWithInputStatus(Long userId, String status);
}
