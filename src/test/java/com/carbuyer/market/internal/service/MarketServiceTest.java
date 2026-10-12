package com.carbuyer.market.internal.service;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import com.carbuyer.market.internal.repository.MarketListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarketServiceTest {

    @Mock
    private CarFacade carFacade;
    @Mock
    private MarketListingRepository repository;
    @InjectMocks
    private MarketService service;

    @Test
    void createListing_shouldSaveMarketListing_whenSellerIsCarOwner() {
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("100");
        Long sellerId = 1L;
        MarketListing exampleListing = new MarketListing(carId, sellerId, null, price);
        CreateListingRequestDto exampleRequest = new CreateListingRequestDto(carId, price);
        when(carFacade.isCarOwner(carId, sellerId)).thenReturn(true);
        when(repository.save(any(MarketListing.class))).thenReturn(exampleListing);

        service.createListing(sellerId, exampleRequest);

        ArgumentCaptor<MarketListing> marketListingArgumentCaptor = ArgumentCaptor.forClass(MarketListing.class);
        verify(repository).save(marketListingArgumentCaptor.capture());

        MarketListing marketListing = marketListingArgumentCaptor.getValue();
        assertThat(marketListing.getCarId()).isEqualTo(carId);
        assertThat(marketListing.getPrice()).isEqualTo(price);
        assertThat(marketListing.getSellerId()).isEqualByComparingTo(sellerId);
    }

    @Test
    void createListing_shouldThrowIllegalArgumentException_whenSellerIsNotCarOwner() {
        UUID carId = UUID.randomUUID();
        Long sellerId = 1L;
        BigDecimal price = new BigDecimal("100");
        CreateListingRequestDto exampleRequest = new CreateListingRequestDto(carId, price);
        when(carFacade.isCarOwner(carId, sellerId)).thenReturn(false);

        assertThatThrownBy(() -> service.createListing(sellerId, exampleRequest))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void cancelListing_shouldChangeMarketListingStatusToCancelled_whenSellerIsCarOwner() {
        UUID exampleListingId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("100");
        Long sellerId = 1L;
        MarketListing exampleListing = new MarketListing(carId, sellerId, null, price);
        when(repository.findById(exampleListingId)).thenReturn(Optional.of(exampleListing));

        service.cancelListing(sellerId, exampleListingId);

        assertThat(exampleListing.getStatus()).isEqualTo(ListingStatus.CANCELLED);
    }

    @Test
    void cancelListing_shouldThrowIllegalArgumentException_whenSellerIsNotCarOwner() {
        UUID exampleListingId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("100");
        Long sellerId = 1L;
        Long ownerId = 2L;
        MarketListing exampleListing = new MarketListing(carId, ownerId, null, price);
        when(repository.findById(exampleListingId)).thenReturn(Optional.of(exampleListing));

        assertThatThrownBy(() -> service.cancelListing(sellerId, exampleListingId))
                .isInstanceOf(RuntimeException.class);
    }
}
