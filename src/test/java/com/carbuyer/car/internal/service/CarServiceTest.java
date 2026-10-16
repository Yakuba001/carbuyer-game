package com.carbuyer.car.internal.service;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.repository.CarRepository;
import com.carbuyer.exception.exceptions.CarNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CarServiceTest {

    @Mock
    private CarRepository carRepository;
    @InjectMocks
    private CarService service;

    @Test
    void isCarOwner_shouldReturnTrue_whenCarExistsOwnerAndSellerIdNotNullAndThisIdsTheSame() {
        UUID carId = UUID.randomUUID();
        Long ownerId = 1L;
        Long sellerId = 1L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), ownerId);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(carId, sellerId);

        assertThat(result).isTrue();
    }

    @Test
    void isCarOwner_shouldThrowIllegalArgumentException_whenCarDoesNotExist() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        assertThatThrownBy(() -> service.isCarOwner(carId, sellerId))
                .isInstanceOf(CarNotFoundException.class);
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenSellerIdIsNull() {
        UUID carId = UUID.randomUUID();
        Long ownerId = 1L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), ownerId);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(carId, null);

        assertThat(result).isFalse();
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenOwnerIdIsNull() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), null);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(carId, sellerId);

        assertThat(result).isFalse();
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenSellerIdIsNotEqualOwnerId() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        Long ownerId = 2L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), ownerId);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = service.isCarOwner(carId, sellerId);

        assertThat(result).isFalse();
    }

    @Test
    void changeCarOwner_shouldReturnFalse_whenRepositoryReturnZero() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        Long ownerId = 2L;
        when(carRepository.setNewCarOwner(ownerId, sellerId, carId)).thenReturn(0);

        boolean result = service.changeCarOwner(ownerId, sellerId, carId);

        assertThat(result).isFalse();
    }

    @Test
    void changeCarOwner_shouldReturnTrue_whenRepositoryReturnOne() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        Long ownerId = 2L;
        when(carRepository.setNewCarOwner(ownerId, sellerId, carId)).thenReturn(1);

        boolean result = service.changeCarOwner(ownerId, sellerId, carId);

        assertThat(result).isTrue();
    }
}
