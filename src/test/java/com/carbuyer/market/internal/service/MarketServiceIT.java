package com.carbuyer.market.internal.service;

import com.carbuyer.car.api.CarFacade;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import com.carbuyer.market.internal.repository.MarketListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@Transactional
public class MarketServiceIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Autowired
    private MarketListingRepository repository;
    @MockBean
    private CarFacade carFacade;
    @Autowired
    private MarketService marketService;

    @Test
    void createListing_shouldSaveNewMarketListingEntityAndReturnHisId_whenSellerIsCarOwner() {
        Long sellerId = 1L;
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("123.32");
        CreateListingRequestDto request = new CreateListingRequestDto(carId, price);
        when(carFacade.isCarOwner(carId, sellerId)).thenReturn(true);

        UUID result = marketService.createListing(sellerId, request);
        MarketListing savedListing = repository.findById(result)
                .orElseThrow(() -> new IllegalArgumentException("Car with this id does not exists"));

        assertThat(result).isNotNull();
        assertThat(savedListing.getCarId()).isEqualTo(carId);
        assertThat(savedListing.getPrice()).isEqualTo(price);
        assertThat(savedListing.getSellerId()).isEqualTo(sellerId);
        assertThat(savedListing.getCreatedAt()).isNotNull();
    }

    @Test
    void cancelListing_shouldChangeListingStatusToCancelled_whenSellerIsCarOwnerAndListingExists() {
        Long sellerId = 1L;
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("123.32");
        CreateListingRequestDto request = new CreateListingRequestDto(carId, price);
        when(carFacade.isCarOwner(carId, sellerId)).thenReturn(true);
        UUID startId = marketService.createListing(sellerId, request);
        MarketListing startEntity = repository.findById(startId)
                .orElseThrow(() -> new IllegalArgumentException("Listing with id: " + startId + " not found"));
        ListingStatus startStatus = startEntity.getStatus();

        marketService.cancelListing(sellerId, startId);
        ListingStatus resultStatus = startEntity.getStatus();

        assertThat(startStatus).isEqualTo(ListingStatus.ACTIVE);
        assertThat(resultStatus).isEqualTo(ListingStatus.CANCELLED);
    }

    @Test
    void cancelListing_shouldThrowIllegalArgumentException_whenListingDoesNotExist() {
        Long sellerId = 1L;
        UUID listingId = UUID.randomUUID();
        assertThatThrownBy(() -> marketService.cancelListing(sellerId, listingId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
