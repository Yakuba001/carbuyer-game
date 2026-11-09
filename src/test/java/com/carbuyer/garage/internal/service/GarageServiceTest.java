package com.carbuyer.garage.internal.service;

import com.carbuyer.exception.exceptions.GarageSlotNotFoundException;
import com.carbuyer.exception.exceptions.NoEmptySlotsException;
import com.carbuyer.exception.exceptions.NotCarOwnerException;
import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GarageServiceTest {

    @Mock
    private GarageSlotRepository garageSlotRepository;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @InjectMocks
    private GarageService garageService;

    @Test
    void getGarageSlotIdWithEmptyStatus_shouldReturnEmptyGarageSlotId_whenExistsByCurrentUserIdByUserId() {
        long userId = 1L;
        int slotNumber = 1;
        UUID carId = UUID.randomUUID();
        String status = "EMPTY";
        GarageSlot exampleSlot = new GarageSlot(userId, slotNumber, carId, GarageSlotStatus.valueOf(status));
        when(garageSlotRepository.findFirstByUserIdAndStatus(anyLong(), any(GarageSlotStatus.class)))
                .thenReturn(Optional.of(exampleSlot));

        Long result = garageService.getGarageSlotIdByUserIdAndStatus(userId, status);

        assertThat(result).isEqualTo(exampleSlot.getId());
    }

    @Test
    void getGarageSlotIdWithEmptyStatus_shouldThrowNoEmptySlotException_whenSlotIdByUserDoesNotExist() {
        long userId = 1L;
        String status = "EMPTY";
        when(garageSlotRepository.findFirstByUserIdAndStatus(anyLong(), any(GarageSlotStatus.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageService.getGarageSlotIdByUserIdAndStatus(userId, status))
                .isInstanceOf(NoEmptySlotsException.class);
    }

    @Test
    void getSellerGarageSlotId_shouldReturnGarageSlotId_whenSlotExistsBySellerAndCarId() {
        long userId = 1L;
        UUID carId = UUID.randomUUID();
        int slotNumber = 1;
        GarageSlot exampleSlot = new GarageSlot(userId, slotNumber, carId, GarageSlotStatus.OCCUPIED);
        when(garageSlotRepository.findByUserIdAndCarId(userId, carId)).thenReturn(Optional.of(exampleSlot));

        Long result = garageService.getSellerGarageSlotId(userId, carId);

        assertThat(result).isEqualTo(exampleSlot.getId());
    }

    @Test
    void getSellerGarageSlotId_shouldThrowNotCarOwnerException_whenSellerDontOwnThisCar() {
        long userId = 1L;
        UUID carId = UUID.randomUUID();
        when(garageSlotRepository.findByUserIdAndCarId(userId, carId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageService.getSellerGarageSlotId(userId, carId))
                .isInstanceOf(NotCarOwnerException.class);
    }

    @Test
    void garageSlotStatusesAndCarIdInItChanger_shouldChangeGaragesStatusesAndCarIdInSlots_whenSlotsExists() {
        long buyerEmptyGarageSlotId = 1L;
        long sellerOccupiedGarageSlotId = 2L;
        UUID carId = UUID.randomUUID();
        int slotNumber = 1;
        GarageSlot buyerSlot = new GarageSlot(buyerEmptyGarageSlotId, slotNumber, null, GarageSlotStatus.EMPTY);
        GarageSlot sellerSlot = new GarageSlot(sellerOccupiedGarageSlotId, slotNumber, carId, GarageSlotStatus.OCCUPIED);
        when(garageSlotRepository.findById(buyerEmptyGarageSlotId)).thenReturn(Optional.of(buyerSlot));
        when(garageSlotRepository.findById(sellerOccupiedGarageSlotId)).thenReturn(Optional.of(sellerSlot));

        garageService.garageSlotStatusesAndCarIdInItChanger(buyerEmptyGarageSlotId, sellerOccupiedGarageSlotId, carId);

        assertThat(buyerSlot.getStatus()).isEqualTo(GarageSlotStatus.OCCUPIED);
        assertThat(sellerSlot.getStatus()).isEqualTo(GarageSlotStatus.EMPTY);
        assertThat(buyerSlot.getCarId()).isEqualTo(carId);
        assertThat(sellerSlot.getCarId()).isEqualTo(null);
    }

    @Test
    void garageSlotStatusesAndCarIdInItChanger_shouldThrowGarageSlotNotFoundException_whenSlotDoesNotExist() {
        long userId = 1L;
        UUID carId = UUID.randomUUID();
        when(garageSlotRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> garageService
                .garageSlotStatusesAndCarIdInItChanger(userId, userId, carId))
                .isInstanceOf(GarageSlotNotFoundException.class);

        verify(garageSlotRepository).findById(userId);
    }

    @Test
    void changeGarageSlotStatusAndSetCarInItByCarIds_shouldNotThrowException_whenCollectionsSizeTheSame() {
        int[] expectedCounts = new int[] {1, 1};
        List<Long> userIds = List.of(1L, 2L);
        List<UUID> carIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(jdbcTemplate.batchUpdate(any(String.class), any(MapSqlParameterSource[].class)))
                .thenReturn(expectedCounts);

        assertDoesNotThrow(() -> garageService.changeGarageSlotStatusAndSetCarInItByCarIds(userIds, carIds));

        verify(jdbcTemplate).batchUpdate(any(String.class), any(MapSqlParameterSource[].class));
    }

    @Test
    void changeGarageSlotStatusAndSetCarInItByCarIds_shouldThrowGarageSlotNotFoundException_whenArgSizesNotSame() {
        List<Long> userIds = List.of(1L, 2L, 3L);
        List<UUID> carIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> garageService.changeGarageSlotStatusAndSetCarInItByCarIds(userIds, carIds))
                .isInstanceOf(GarageSlotNotFoundException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void changeGarageSlotStatusAndSetCarInItByCarIds_shouldThrowGarageSlotNotFoundException_whenWereUpdatedNotSameAmt() {
        int[] expectedCounts = new int[] {1, 2};
        List<Long> userIds = List.of(1L, 2L);
        List<UUID> carIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(jdbcTemplate.batchUpdate(any(String.class), any(MapSqlParameterSource[].class)))
                .thenReturn(expectedCounts);

        assertThatThrownBy(() -> garageService.changeGarageSlotStatusAndSetCarInItByCarIds(userIds, carIds))
                .isInstanceOf(GarageSlotNotFoundException.class);

        verify(jdbcTemplate).batchUpdate(any(String.class), any(MapSqlParameterSource[].class));
    }

    @Test
    void addGarageSlot_shouldAddedGarageSlotAndInvokeRepoSave_whenInputDataIsValid() {
        long userId = 1L;
        int slotNumber = 1;
        GarageSlot firstSlot = new GarageSlot(userId, slotNumber, null, GarageSlotStatus.EMPTY);
        when(garageSlotRepository.save(any())).thenReturn(firstSlot);

        garageService.addGarageSlot(userId, slotNumber);

        verify(garageSlotRepository).save(any());
    }

    @Test
    void addGarageSlots_shouldAddedGarageSlotsAndInvokeRepoSaveAllAndFlush_whenInputDataIsValid() {
        int[] expectedCounts = new int[] {1, 1};
        List<Long> userIds = List.of(1L, 2L);
        when(jdbcTemplate.batchUpdate(any(String.class), any(MapSqlParameterSource[].class))).thenReturn(expectedCounts);

        garageService.addGarageSlots(userIds);

        verify(jdbcTemplate).batchUpdate(any(String.class), any(MapSqlParameterSource[].class));
    }
}
