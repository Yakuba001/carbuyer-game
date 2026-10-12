package com.carbuyer.car.internal;

import com.carbuyer.car.internal.entity.Car;
import com.carbuyer.car.internal.repository.CarRepository;
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
public class CarFacadeImplTest {

    @Mock
    private CarRepository carRepository;
    @InjectMocks
    private CarFacadeImpl carFacade;

    @Test
    void isCarOwner_shouldReturnTrue_whenCarExistsOwnerAndSellerIdNotNullAndThisIdsTheSame() {
        UUID carId = UUID.randomUUID();
        Long ownerId = 1L;
        Long sellerId = 1L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), ownerId);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = carFacade.isCarOwner(carId, sellerId);

        assertThat(result).isTrue();
    }

    @Test
    void isCarOwner_shouldThrowIllegalArgumentException_whenCarDoesNotExist() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        assertThatThrownBy(() -> carFacade.isCarOwner(carId, sellerId)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenSellerIdIsNull() {
        UUID carId = UUID.randomUUID();
        Long ownerId = 1L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), ownerId);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = carFacade.isCarOwner(carId, null);

        assertThat(result).isFalse();
    }

    @Test
    void isCarOwner_shouldReturnFalse_whenOwnerIdIsNull() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        Car exampleCar =
                new Car("228", "Opel", 100, 50, new BigDecimal("200"), null);
        when(carRepository.findById(carId)).thenReturn(Optional.of(exampleCar));

        boolean result = carFacade.isCarOwner(carId, sellerId);

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

        boolean result = carFacade.isCarOwner(carId, sellerId);

        assertThat(result).isFalse();
    }
}
