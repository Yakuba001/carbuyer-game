package com.carbuyer.garage.internal.listener;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import com.carbuyer.user.api.event.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserCreatedEventListenerTest {

    @Mock
    private GarageSlotRepository garageSlotRepository;

    @InjectMocks
    private UserCreatedEventListener listener;

    @Captor
    private ArgumentCaptor<List<GarageSlot>> slotsCaptor;

    @Test
    void handleUserCreated_ShouldSaveCorrectNumberOfSlots() {
        Long userId = 1L;
        int maxSlots = 3;
        UserCreatedEvent event = new UserCreatedEvent(userId, maxSlots);

        listener.handleUserCreated(event);

        verify(garageSlotRepository).saveAll(slotsCaptor.capture());
        List<GarageSlot> savedSlots = slotsCaptor.getValue();

        assertThat(savedSlots).hasSize(3);
        assertThat(savedSlots)
                .extracting(GarageSlot::getUserId, GarageSlot::getSlotNumber, GarageSlot::getStatus)
                .containsExactly(
                        tuple(userId, 1, GarageSlotStatus.EMPTY),
                        tuple(userId, 2, GarageSlotStatus.EMPTY),
                        tuple(userId, 3, GarageSlotStatus.EMPTY)
                );
    }

    @Test
    void handleUserCreated_ShouldSaveEmptyList_WhenMaxSlotsIsZero() {
        UserCreatedEvent event = new UserCreatedEvent(1L, 0);

        listener.handleUserCreated(event);

        verify(garageSlotRepository).saveAll(slotsCaptor.capture());
        assertThat(slotsCaptor.getValue()).isEmpty();
    }
}
