package com.carbuyer.user.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.UserBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Import(UserBalanceRepositoryIT.UserBalanceRepositoryTest.class)
public class UserBalanceRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private UserBalanceRepository userBalanceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserBalanceRepositoryTest testService;

    @Test
    void checkCascadeDeleting_shouldDeleteUserBalance_whenUserWithSameUserIdIsDeleted() {
        long userId = 1L;
        BigDecimal balance = new BigDecimal("123.32");
        addUserAndHisBalanceToDb(userId, balance);

        boolean isUserBalanceExistsBeforeUserDeleted = userBalanceRepository.existsById(userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        boolean isUserBalanceExistsAfterUserDeletion = userBalanceRepository.existsById(userId);

        assertThat(isUserBalanceExistsBeforeUserDeleted).isTrue();
        assertThat(isUserBalanceExistsAfterUserDeletion).isFalse();
    }

    @Test
    void minusBalanceIfSufficient_shouldReturnZero_whenUserDoesNotHaveEnoughBalance() {
        long userId = 1L;
        BigDecimal balance = new BigDecimal("123.32");
        BigDecimal price = new BigDecimal("222.33");
        addUserAndHisBalanceToDb(userId, balance);

        int result = userBalanceRepository.minusBalanceIfSufficient(userId, price);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void minusBalanceIfSufficient_shouldReturnOneAndSetUserBalanceMinusPrice_whenUserHasEnoughBalance() {
        long userId = 1L;
        BigDecimal balance = new BigDecimal("123.32");
        BigDecimal price = new BigDecimal("22.33");
        BigDecimal expectedPrice = balance.subtract(price);
        addUserAndHisBalanceToDb(userId, balance);

        int result = userBalanceRepository.minusBalanceIfSufficient(userId, price);
        UserBalance userBalance = userBalanceRepository.findById(userId).orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(userBalance.getBalance()).isEqualTo(expectedPrice);
    }

    @Test
    void plusBalance_shouldReturnOneAndSetUserBalancePlusPrice() {
        long userId = 1L;
        BigDecimal balance = new BigDecimal("123.32");
        BigDecimal price = new BigDecimal("22.33");
        BigDecimal expectedPrice = balance.add(price);
        addUserAndHisBalanceToDb(userId, balance);

        int result = userBalanceRepository.plusBalance(userId, price);
        UserBalance userBalance = userBalanceRepository.findById(userId).orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(userBalance.getBalance()).isEqualTo(expectedPrice);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findBalanceByUserId_shouldChangeUserBalanceOnce_whenEnvironmentIsConcurrency() {
        long userId = 1L;
        long delayMs = 100L;
        BigDecimal oldBalance = new BigDecimal("123.32");
        addUserAndHisBalanceToDb(userId, oldBalance);
        BigDecimal newBalance = new BigDecimal("222.33");
        CountDownLatch startLatch = new CountDownLatch(1);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        List<Boolean> resultList;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            for (int i = 0; i < 5; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        startLatch.await();
                        return testService.updateUserBalanceWithDelay(userId, oldBalance, newBalance, delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }, executor));
            }
            startLatch.countDown();
            resultList = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(el -> el == true)
                    .toList();
        }

        assertThat(resultList).hasSize(1);

        dataBaseCleanup(userId);
    }

    @TestComponent
    public static class UserBalanceRepositoryTest {

        @Autowired
        private UserBalanceRepository userBalanceRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public boolean updateUserBalanceWithDelay(Long userId, BigDecimal oldBalance, BigDecimal newBalance, long delay) {
            UserBalance userBalance = userBalanceRepository.findBalanceByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("UserBalance not found"));
            if (userBalance.getBalance().compareTo(oldBalance) != 0) {
                return false;
            }
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            userBalance.setBalance(newBalance);
            userBalanceRepository.saveAndFlush(userBalance);
            return true;
        }
    }

    private void dataBaseCleanup(long userId) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }

    private void addUserAndHisBalanceToDb(long userId, BigDecimal balance) {
        String username = "Bill";
        String email = "billGreat@g.com";
        String password = "password";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                userId, username, email, password);
        jdbcTemplate.update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", userId, balance);
    }
}
