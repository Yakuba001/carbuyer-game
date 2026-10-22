package com.carbuyer.market.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class MarketListingRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private MarketListingRepository marketListingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final static Long TEST_USER_ID = 1L;
    private final static UUID TEST_CAR_ID = UUID.randomUUID();
    private final static BigDecimal testPrice = new BigDecimal("100");

    @Test
    void findAllByStatus_shouldReturnOnlyActiveListings_andCheckAutoFillingDefaultField() {
        addUserToDb();
        BigDecimal price = new BigDecimal("100");
        UUID firstCarId = UUID.randomUUID();
        UUID secondCarId = UUID.randomUUID();
        UUID thirdCarId = UUID.randomUUID();
        addTestCorrectCarToDb(firstCarId);
        addTestCorrectCarToDb(secondCarId);
        addTestCorrectCarToDb(thirdCarId);

        MarketListing exampleListing1 = new MarketListing(firstCarId, TEST_USER_ID, null, price);
        MarketListing exampleListing2 = new MarketListing(secondCarId, TEST_USER_ID, null, price);
        MarketListing exampleListing3 = new MarketListing(thirdCarId, TEST_USER_ID, null, price);
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
        assertThat(firstElement.getSellerId()).isEqualTo(TEST_USER_ID);
        assertThat(firstElement.getBuyerId()).isNull();
        assertThat(firstElement.getPrice()).isEqualTo(price);
        assertThat(firstElement.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(firstElement.getCreatedAt()).isNotNull();
        assertThat(firstElement.getClosedAt()).isNull();
    }

    @Test
    void checkConstraint_shouldThrowException_whenPriceNegative() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        exampleListing.setPrice(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrowException_whenStatusIsNotActiveOrSoldOrCancelled() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE market_listings SET status = 'BROKEN' " +
                "WHERE seller_id = ?", TEST_USER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrowException_whenStatusActiveAndClosedAtIsNotNull() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        exampleListing.setClosedAt(Instant.now());

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrowException_whenStatusSoldOrCancelledAndClosedAtIsNull() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        exampleListing.setStatus(ListingStatus.CANCELLED);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrowException_whenBuyerIsSeller() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(TEST_USER_ID);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrowException_whenDeleteUserWhichHaveMarketListings() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", TEST_USER_ID))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrowException_whenDeleteCarWhichUseInMarketListings() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);
        UUID carId = exampleListing.getCarId();

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM cars WHERE id = ?", carId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @MethodSource("nullFieldsInListingObjs")
    void checkConstraintNotNullFields_shouldThrowException_whenSendListingWithRequirementNonNullFields(
            MarketListing listing) {
        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(listing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkUniqueIndex_shouldWorkCorrectly_whenTableHasSomeListingsWithTheSameCarIdButDiffStatus() {
        long expectedCount = 3L;
        MarketListing activeStatusListing = addToDbAndGetTestMarketListing(null);
        MarketListing cancelStatusListing = new MarketListing(TEST_CAR_ID, TEST_USER_ID, null, testPrice);
        cancelStatusListing.setStatus(ListingStatus.CANCELLED);
        cancelStatusListing.setClosedAt(Instant.now());
        MarketListing soldStatusListing = new MarketListing(TEST_CAR_ID, TEST_USER_ID, null, testPrice);
        soldStatusListing.setStatus(ListingStatus.SOLD);
        soldStatusListing.setClosedAt(Instant.now());

        marketListingRepository.saveAll(List.of(activeStatusListing, cancelStatusListing, soldStatusListing));
        long result = marketListingRepository.count();

        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    void checkUniqueIndex_shouldThrowException_whenTableHasTwoActiveListingsWithSameCarId() {
        MarketListing activeStatusListing = addToDbAndGetTestMarketListing(null);
        MarketListing secondActiveWithSameCarId = new MarketListing(TEST_CAR_ID, TEST_USER_ID, null, testPrice);
        marketListingRepository.saveAndFlush(activeStatusListing);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(secondActiveWithSameCarId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static Stream<MarketListing> nullFieldsInListingObjs() {
        MarketListing nullStatus = new MarketListing(TEST_CAR_ID, TEST_USER_ID, null, testPrice);
        nullStatus.setStatus(null);

        return Stream.of(
                new MarketListing(null, TEST_USER_ID, null, testPrice),
                new MarketListing(TEST_CAR_ID, null, null, testPrice),
                new MarketListing(TEST_CAR_ID, TEST_USER_ID, null, null),
                nullStatus
        );
    }

    private MarketListing addToDbAndGetTestMarketListing(Long buyerId) {
        addUserToDb();
        addTestCorrectCarToDb(TEST_CAR_ID);
        return new MarketListing(TEST_CAR_ID, TEST_USER_ID, buyerId, testPrice);
    }

    private void addTestCorrectCarToDb(UUID carId) {
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int mileage = 10000;
        int conditionScore = 85;
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                carId, randomVin, model, mileage, conditionScore, testPrice, TEST_USER_ID);
    }

    private void addUserToDb() {
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                TEST_USER_ID, username, userEmail, userPassword);
    }
}
