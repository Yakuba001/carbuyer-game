package com.carbuyer.market.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@Import(MarketListingRepositoryIT.MarketListingTestService.class)
public class MarketListingRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private MarketListingRepository marketListingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MarketListingTestService testService;

    private final static Long testUserId = 1L;
    private final static UUID testCarId = UUID.randomUUID();
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

        MarketListing exampleListing1 = new MarketListing(firstCarId, testUserId, null, price);
        MarketListing exampleListing2 = new MarketListing(secondCarId, testUserId, null, price);
        MarketListing exampleListing3 = new MarketListing(thirdCarId, testUserId, null, price);
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
        assertThat(firstElement.getSellerId()).isEqualTo(testUserId);
        assertThat(firstElement.getBuyerId()).isNull();
        assertThat(firstElement.getPrice()).isEqualTo(price);
        assertThat(firstElement.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(firstElement.getCreatedAt()).isNotNull();
        assertThat(firstElement.getClosedAt()).isNull();
    }

    @Test
    void checkConstraint_shouldThrownException_whenPriceNegative() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        exampleListing.setPrice(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenStatusIsNotActiveOrSoldOrCancelled() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE market_listings SET status = 'BROKEN' " +
                "WHERE seller_id = ?", testUserId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenStatusActiveAndClosedAtIsNotNull() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        exampleListing.setClosedAt(Instant.now());

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenStatusSoldOrCancelledAndClosedAtIsNull() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        exampleListing.setStatus(ListingStatus.CANCELLED);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenBuyerIsSeller() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(testUserId);

        assertThatThrownBy(() -> marketListingRepository.saveAndFlush(exampleListing))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenDeleteUserWhichHaveMarketListings() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", testUserId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_shouldThrownException_whenDeleteCarWhichUseInMarketListings() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);
        UUID carId = exampleListing.getCarId();

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM cars WHERE id = ?", carId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findByIdForUpdate_shouldUpdateEntityOnlyOneTime_whenEnvironmentIsConcurrency() {
        MarketListing exampleListing = addToDbAndGetTestMarketListing(null);
        marketListingRepository.saveAndFlush(exampleListing);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        UUID listingId = exampleListing.getId();
        ListingStatus oldStatus = ListingStatus.ACTIVE;
        ListingStatus newStatus = ListingStatus.SOLD;
        long delayMs = 100L;

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        List<Boolean> resultList;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        countDownLatch.await();
                        return testService.updateListingStatusWithDelay(listingId, oldStatus, newStatus, delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }, executor));
            }
            countDownLatch.countDown();

            resultList = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(el -> el == true)
                    .toList();
        }

        assertThat(resultList).hasSize(1);

        dataBaseCleanup(listingId);
    }

    @TestComponent
    public static class MarketListingTestService {

        @Autowired
        private MarketListingRepository marketListingRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public boolean updateListingStatusWithDelay(UUID id,
                                                    ListingStatus oldStatus,
                                                    ListingStatus newStatus,
                                                    long delayMs) {
            MarketListing listing = marketListingRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
            if (!listing.getStatus().equals(oldStatus)) {
                return false;
            }
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            listing.setStatus(newStatus);
            listing.setClosedAt(Instant.now());
            marketListingRepository.saveAndFlush(listing);
            return true;
        }
    }

    private void dataBaseCleanup(UUID listingId) {
        marketListingRepository.deleteById(listingId);

        jdbcTemplate.update("DELETE FROM cars WHERE id = ?", testCarId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", testUserId);
    }

    private MarketListing addToDbAndGetTestMarketListing(Long buyerId) {
        addUserToDb();
        addTestCorrectCarToDb(testCarId);
        return new MarketListing(testCarId, testUserId, buyerId, testPrice);
    }

    private void addTestCorrectCarToDb(UUID carId) {
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int mileage = 10000;
        int conditionScore = 85;
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                carId, randomVin, model, mileage, conditionScore, testPrice, testUserId);
    }

    private void addUserToDb() {
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                testUserId, username, userEmail, userPassword);
    }
}
