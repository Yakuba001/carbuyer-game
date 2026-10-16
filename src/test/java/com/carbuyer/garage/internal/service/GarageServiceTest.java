package com.carbuyer.garage.internal.service;

import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void isHasGarageSlotWithInputStatus_shouldReturnFalse_whenStatusIsNull() {
        long userId = 1L;

        boolean result = garageService.isHasGarageSlotWithInputStatus(userId, null);

        assertThat(result).isFalse();
        verifyNoInteractions(garageSlotRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "SomeText"})
    void isHasGarageSlotWithInputStatus_shouldReturnFalse_whenStatusInvalidOrEmpty(String status) {
        long userId = 1L;

        boolean result = garageService.isHasGarageSlotWithInputStatus(userId, status);

        assertThat(result).isFalse();
        verifyNoInteractions(garageSlotRepository);
    }

    @Test
    void isHasGarageSlotWithInputStatus_shouldReturnTrue_whenStatusIsValidAndSlotExistsAndEditingInputString() {
        long userId = 1L;
        String status = " EmPtY";
        when(garageSlotRepository.existsByUserIdAndStatus(anyLong(), any(GarageSlotStatus.class))).thenReturn(true);

        boolean result = garageService.isHasGarageSlotWithInputStatus(userId, status);

        assertThat(result).isTrue();
    }
}
