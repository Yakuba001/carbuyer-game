package com.carbuyer.garage.internal.listener;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import com.carbuyer.user.api.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
class UserCreatedEventListener {

    private final GarageSlotRepository garageSlotRepository;

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void handleUserCreated(UserCreatedEvent event) {
        List<GarageSlot> garageSlots = new ArrayList<>(event.maxSlots());
        for (int i = 1; i <= event.maxSlots(); i++) {
            garageSlots.add(new GarageSlot(
                    event.userId(),
                    i,
                    null,
                    GarageSlotStatus.EMPTY
            ));
        }
        garageSlotRepository.saveAll(garageSlots);
    }
}
