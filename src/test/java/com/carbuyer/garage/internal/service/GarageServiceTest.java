package com.carbuyer.garage.internal.service;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GarageServiceTest {

    @Mock
    private GarageSlotRepository garageSlotRepository;
    @InjectMocks
    private GarageService garageService;

    // TODO: rewrite this....................

//    @Test
//    void isHasGarageSlotWithInputStatus_shouldReturnFalse_whenStatusIsNullId() {
//        long userId = 1L;
//
//        boolean result = garageService.getGarageSlotIdWithEmptyStatus(userId, null);
//
//        assertThat(result).isFalse();
//        verifyNoInteractions(garageSlotRepository);
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"", " ", "SomeText"})
//    void getGarageSlotIdWithEmptyStatus_shouldReturnFalse_whenStatusInvalidOrEmpty(String status) {
//        long userId = 1L;
//
//        boolean result = garageService.getGarageSlotIdWithEmptyStatus(userId, status);
//
//        assertThat(result).isFalse();
//        verifyNoInteractions(garageSlotRepository);
//    }
//
//    @Test
//    void isHasGarageSlotWithInputStatus_shouldReturnTrue_whenStatusIsValidAndSlotIdExistsAndEditingEmptyString() {
//        long userId = 1L;
//        String status = " EmPtY";
//        UUID carId = UUID.randomUUID();
//        int slotNumber = 1;
//        GarageSlotStatus testStatus = GarageSlotStatus.EMPTY;
//        GarageSlot garageSlot = new GarageSlot(userId, slotNumber, carId, testStatus);
//        when(garageSlotRepository.findFirstByUserIdAndStatus(anyLong(), any(GarageSlotStatus.class)))
//                .thenReturn(Optional.of(garageSlot));
//
//        boolean result = garageService.getGarageSlotIdWithEmptyStatus(userId, status);
//
//        assertThat(result).isTrue();
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {"", " ", "SomeText"})
//    void isChangedStatusFromEmptyToOccupiedAndSetCarId_shouldReturnFalse_whenStatusInvalidOrEmpty(String status) {
//        long userId = 1L;
//        UUID carID = UUID.randomUUID();
//
//        boolean result = garageService.isChangedStatusFromEmptyToOccupiedAndSetCarId(userId, carID, status);
//
//        assertThat(result).isFalse();
//    }
//
//    @Test
//    void isChangedStatusFromEmptyToOccupiedAndSetCarId_shouldReturnFalse_whenStatusIsNull() {
//        long userId = 1L;
//        UUID carID = UUID.randomUUID();
//
//        boolean result = garageService.isChangedStatusFromEmptyToOccupiedAndSetCarId(userId, carID, null);
//
//        assertThat(result).isFalse();
//    }
//
//    @Test
//    void isChangedStatusFromEmptyToOccupiedAndSetCarId_shouldReturnTrue_whenStatusIsValidAndSlotExistsAndTrimmed() {
//        long userId = 1L;
//        UUID carID = UUID.randomUUID();
//        String status = " OcCuPieD   ";
//        when(garageSlotRepository.occupyFirstEmptySlot(anyLong(), any(UUID.class), any(String.class)))
//                .thenReturn(1);
//
//        boolean result = garageService.isChangedStatusFromEmptyToOccupiedAndSetCarId(userId, carID, status);
//
//        assertThat(result).isTrue();
//    }
}
