package com.carbuyer.market.internal.service;

import com.carbuyer.containers.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class MarketServiceIT extends BaseIntegrationTest {

    @Autowired
    private MarketService marketService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long SELLER_ID = 0L;
    private static final UUID CAR_ID = UUID.randomUUID();
    private static final UUID LISTING_ID = UUID.randomUUID();

    private static final int THREAD_COUNT = 51;

    // TODO: check what happened in data base after success results...........

    @Test
    void buyCar_shouldPreventRaceCondition_whenMultipleUsersBuySameListing() {
        addUsersToDb();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(THREAD_COUNT);
        boolean finished = false;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            for (int i = 1; i <= THREAD_COUNT; i++) {
                long finalI = i;
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        marketService.buyCar(finalI, LISTING_ID);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        finishLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            try {
                finished = finishLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        assertThat(finished).isTrue();
        assertThat(failureCount.get()).isEqualTo(THREAD_COUNT - 1);
    }

    private void addUsersToDb() {
        String password = "123";
        int slotNumber = 1;
        BigDecimal userBalance = new BigDecimal("100");
        String randomVin1 = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int mileage = 10000;
        int conditionScore = 85;
        BigDecimal price = new BigDecimal("123.21");
        BigDecimal listingPrice = new BigDecimal("50");
        List<String> usernames = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> "name" + i)
                .toList();
        List<String> emails = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> "email" + i + "@g.c")
                .toList();
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                SELLER_ID, usernames.getFirst(), emails.getFirst(), password);
        jdbcTemplate.update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", SELLER_ID, userBalance);
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", CAR_ID, randomVin1, model, mileage, conditionScore, price, SELLER_ID);
        jdbcTemplate.update("INSERT INTO garage_slots (user_id, slot_number, car_id, status) VALUES (?, ?, ?, ?)",
                SELLER_ID, slotNumber, CAR_ID, "OCCUPIED");
        jdbcTemplate.update("INSERT INTO market_listings (id, car_id, seller_id, price) OVERRIDING SYSTEM VALUE " +
                "VALUES (?, ?, ?, ?)", LISTING_ID, CAR_ID, SELLER_ID, listingPrice);

        for (int i = 1; i < THREAD_COUNT; i++) {
            jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                            "VALUES (?, ?, ?, ?)",
                    i, usernames.get(i), emails.get(i), password);
            jdbcTemplate.update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", i, userBalance);
            jdbcTemplate.update("INSERT INTO garage_slots (user_id, slot_number, car_id, status) VALUES (?, ?, ?, ?)",
                    i, slotNumber, null, "EMPTY");
        }
    }
}
