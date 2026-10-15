package com.carbuyer.market.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class MarketListingRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private MarketListingRepository marketListingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findAllByStatus_shouldReturnOnlyActiveListings_andCheckAutoFillingDefaultField() {
        Long sellerId = 1L;
        addUserToDb(sellerId);
        BigDecimal price = new BigDecimal("100");
        UUID firstCarId = UUID.randomUUID();
        UUID secondCarId = UUID.randomUUID();
        UUID thirdCarId = UUID.randomUUID();
        addTestCorrectCarToDb(sellerId, firstCarId, price);
        addTestCorrectCarToDb(sellerId, secondCarId, price);
        addTestCorrectCarToDb(sellerId, thirdCarId, price);

        MarketListing exampleListing1 = new MarketListing(firstCarId, sellerId, null, price);
        MarketListing exampleListing2 = new MarketListing(secondCarId, sellerId, null, price);
        MarketListing exampleListing3 = new MarketListing(thirdCarId, sellerId, null, price);
        List<MarketListing> listings = List.of(exampleListing1, exampleListing2, exampleListing3);
        marketListingRepository.saveAllAndFlush(listings);

        exampleListing3.setStatus(ListingStatus.CANCELLED);
        exampleListing3.setClosedAt(Instant.now());
        marketListingRepository.flush();
        Pageable pageable = PageRequest.of(0, 1);

        Page<MarketListing> resultPage = marketListingRepository.findAllByStatus(ListingStatus.ACTIVE, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getTotalPages()).isEqualTo(2);
        assertThat(resultPage.getContent().size()).isEqualTo(1);

        MarketListing firstElement = resultPage.getContent().getFirst();
        assertThat(firstElement.getId()).isNotNull();
        assertThat(firstElement.getSellerId()).isEqualTo(sellerId);
        assertThat(firstElement.getBuyerId()).isNull();
        assertThat(firstElement.getPrice()).isEqualTo(price);
        assertThat(firstElement.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(firstElement.getCreatedAt()).isNotNull();
        assertThat(firstElement.getClosedAt()).isNull();
    }

    @Test
    void checkConstraint_shouldThrownException_whenPriceNegative() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, null);
        exampleListing.setPrice(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenStatusIsNotActiveOrSoldOrCancelled() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, null);
        marketListingRepository.saveAndFlush(exampleListing);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE market_listings SET status = 'BROKEN' " +
                "WHERE seller_id = ?", sellerId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenStatusActiveAndClosedAtIsNotNull() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, null);
        exampleListing.setClosedAt(Instant.now());

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenStatusSoldOrCancelledAndClosedAtIsNull() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, null);
        exampleListing.setStatus(ListingStatus.CANCELLED);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenBuyerIsSeller() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, sellerId);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenDeleteUserWhichHaveMarketListings() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, null);
        marketListingRepository.saveAndFlush(exampleListing);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", sellerId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenDeleteCarWhichUseInMarketListings() {
        long sellerId = 1L;
        MarketListing exampleListing = addToDbAndGetTestMarketListing(sellerId, null);
        marketListingRepository.saveAndFlush(exampleListing);
        UUID carId = exampleListing.getCarId();

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM cars WHERE id = ?", carId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MarketListing addToDbAndGetTestMarketListing(long sellerId, Long ownerId) {
        UUID carId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("100");
        addUserToDb(sellerId);
        addTestCorrectCarToDb(sellerId, carId, price);
        return new MarketListing(carId, sellerId, ownerId, price);
    }

    private void addTestCorrectCarToDb(long userId, UUID carId, BigDecimal price) {
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int mileage = 10000;
        int conditionScore = 85;
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                carId, randomVin, model, mileage, conditionScore, price, userId);
    }

    private void addUserToDb(long userId) {
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                userId, username, userEmail, userPassword);
    }
}
