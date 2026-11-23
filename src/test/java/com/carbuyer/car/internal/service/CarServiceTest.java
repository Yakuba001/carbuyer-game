package com.carbuyer.car.internal.service;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.repository.CarRepository;
import com.carbuyer.exception.exceptions.CarNotFoundException;
import com.carbuyer.exception.exceptions.EmptyInputCollectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {

    @Mock
    private CarRepository carRepository;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @InjectMocks
    private CarService service;

    private static final UUID TEST_CAR_ID = UUID.randomUUID();
    private static final Long TEST_USER_ID = 1L;

    @Test
    void isCarOwner_shouldReturnTrue_whenCarExistsOwnerAndSellerIdNotNullAndThisIdsTheSame() {
        Car exampleCar =
                new Car("228", "Opel", 100, 50, TEST_USER_ID);
        when(carRepository.findForUpdateById(TEST_CAR_ID)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(TEST_CAR_ID, TEST_USER_ID);

        assertThat(result).isTrue();
    }

    @Test
    void isCarOwner_shouldThrowCarNotFoundException_whenCarDoesNotExist() {
        assertThrows(CarNotFoundException.class, () -> service.isCarOwner(TEST_CAR_ID, TEST_USER_ID));
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenSellerIdIsNull() {
        Car exampleCar =
                new Car("228", "Opel", 100, 50, TEST_USER_ID);
        when(carRepository.findForUpdateById(TEST_CAR_ID)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(TEST_CAR_ID, null);

        assertThat(result).isFalse();
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenOwnerIdIsNull() {
        Car exampleCar =
                new Car("228", "Opel", 100, 50, null);
        when(carRepository.findForUpdateById(TEST_CAR_ID)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(TEST_CAR_ID, TEST_USER_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenSellerIdIsNotEqualOwnerId() {
        Long ownerId = 2L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, ownerId);
        when(carRepository.findForUpdateById(TEST_CAR_ID)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(TEST_CAR_ID, TEST_USER_ID);

        assertThat(result).isFalse();
    }

    @Test
    void changeCarOwner_shouldReturnFalse_whenRepositoryReturnZero() {
        Long ownerId = 2L;
        when(carRepository.setNewCarOwner(ownerId, TEST_USER_ID, TEST_CAR_ID)).thenReturn(0);

        boolean result = service.changeCarOwner(ownerId, TEST_USER_ID, TEST_CAR_ID);

        assertThat(result).isFalse();
    }

    @Test
    void changeCarOwner_shouldReturnTrue_whenRepositoryReturnOne() {
        Long ownerId = 2L;
        when(carRepository.setNewCarOwner(ownerId, TEST_USER_ID, TEST_CAR_ID)).thenReturn(1);

        boolean result = service.changeCarOwner(ownerId, TEST_USER_ID, TEST_CAR_ID);

        assertThat(result).isTrue();
    }

    @Test
    void addCarsAndSetNewOwners_shouldSaveAllCarsInDb() {
        List<Long> ownerIds = List.of(1L, 2L);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        doAnswer(i -> {
            KeyHolder keyHolder = i.getArgument(2);
            keyHolder.getKeyList().add(Map.of("id", uuid1));
            keyHolder.getKeyList().add(Map.of("id", uuid2));
            return new int[]{1, 1};
        }).when(jdbcTemplate).batchUpdate(
                any(String.class),
                any(MapSqlParameterSource[].class),
                any(KeyHolder.class),
                eq(new String[]{"id"}));

        List<UUID> result = service.addCarsAndSetNewOwners(ownerIds);

        assertThat(result.size()).isEqualTo(ownerIds.size());
        assertThat(result.getFirst()).isEqualTo(uuid1);
        assertThat(result.getLast()).isEqualTo(uuid2);
    }

    @Test
    void deleteCarsByOwnerIds_shouldThrowEmptyInputCollectionException_whenOwnerIdsListIsNull() {
        assertThrows(EmptyInputCollectionException.class, () -> service.deleteCarsByOwnerIds(null));
    }

    @Test
    void deleteCarsByOwnerIds_shouldWorksCorrectly_whenInputArgIsValid() {
        List<Long> ownerIds = List.of(1L, 2L);

        service.deleteCarsByOwnerIds(ownerIds);
    }
}
