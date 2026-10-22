package com.carbuyer.market.internal.service;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.NotCarOwnerException;
import com.carbuyer.exception.exceptions.NotListingOwnerException;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.api.dto.MarketListingResponseDto;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import com.carbuyer.market.internal.repository.MarketListingRepository;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
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
    private UserFacade userFacade;
    @Mock
    private GarageFacade garageFacade;
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
                .isInstanceOf(NotCarOwnerException.class);

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
                .isInstanceOf(NotListingOwnerException.class);
    }

    @Test
    void findAllByStatus_shouldCorrectMappedFromRawEntityToResponseDto() {
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("100");
        Long sellerId = 1L;
        PageRequest pageable = PageRequest.of(0, 10);
        MarketListing exampleListing = new MarketListing(carId, sellerId, null, price);
        Page<MarketListing> mockPage = new PageImpl<>(List.of(exampleListing), pageable, 1);
        when(repository.findAllByStatus(ListingStatus.ACTIVE, pageable)).thenReturn(mockPage);

        Page<MarketListingResponseDto> result = service.findAllByStatus(pageable);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        MarketListingResponseDto marketListingResponseDto = result.getContent().getFirst();
        assertThat(marketListingResponseDto).isNotNull();
        assertThat(carId).isEqualTo(marketListingResponseDto.carId());
        assertThat(sellerId).isEqualTo(marketListingResponseDto.sellerId());
        assertThat(price).isEqualTo(marketListingResponseDto.price());
    }

    // TODO: after find the best way to realize this method continue.......

//    @Test
//    void buyCar_shouldCloseListing_whenEverythingIsDone() {
//        long buyerId = 1L;
//        long sellerId = 2L;
//        BigDecimal price = new BigDecimal("100");
//        UUID listingId = UUID.randomUUID();
//        UUID carId = UUID.randomUUID();
//        MarketListing listing = new MarketListing(listingId, sellerId, null, price);
//        listing.setStatus(ListingStatus.ACTIVE);
//        listing.setCarId(carId);
//        BigDecimal buyerBalance = new BigDecimal("100");
//        BigDecimal sellerBalance = new BigDecimal("100");
//        long buyerGarageSlotId = 1L;
//        long sellerGarageSlotId = 2L;
//
//        when(userFacade.existsById(anyLong())).thenReturn(true);
//        when(repository.findByIdForUpdate(any(UUID.class))).thenReturn(Optional.of(listing));
//        when(carFacade.isCarOwner(any(UUID.class), anyLong())).thenReturn(true);
//        when(userFacade.getUserBalanceById(anyLong())).thenReturn(buyerBalance);
//        when(userFacade.getUserBalanceById(anyLong())).thenReturn(sellerBalance);
//        when(garageFacade.getGarageSlotIdWithEmptyStatus(anyLong(), any())).thenReturn(buyerGarageSlotId);
//        when(garageFacade.getGarageSlotIdWithEmptyStatus(anyLong(), any())).thenReturn(sellerGarageSlotId);
//        when(userFacade.carPurchase(
//                anyLong(),
//                anyLong(),
//                any(BigDecimal.class),
//                any(BigDecimal.class),
//                any(BigDecimal.class)))
//                .thenReturn(true);
//        when(carFacade.setNewCarOwner(anyLong(), anyLong(), any(UUID.class))).thenReturn(true);
//        doNothing().when(garageFacade).changeGarageSlotStatusesAndIdInIt(anyLong(), anyLong(), any(UUID.class));
//
//        service.buyCar(buyerId, listingId);
//
//        assertThat(listing.getStatus()).isEqualTo(ListingStatus.SOLD);
//        assertThat(listing.getBuyerId()).isEqualTo(buyerId);
//        assertThat(listing.getClosedAt()).isNotNull();
//    }
}
