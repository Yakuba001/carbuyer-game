package com.carbuyer.user.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.UserBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class UserBalanceRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private UserBalanceRepository userBalanceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        BigDecimal price =  new BigDecimal("222.33");
        addUserAndHisBalanceToDb(userId, balance);

        int result = userBalanceRepository.minusBalanceIfSufficient(userId, price);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void minusBalanceIfSufficient_shouldReturnOneAndSetUserBalanceMinusPrice_whenUserHasEnoughBalance() {
        long userId = 1L;
        BigDecimal balance = new BigDecimal("123.32");
        BigDecimal price =  new BigDecimal("22.33");
        BigDecimal expectedPrice = balance.subtract(price);
        addUserAndHisBalanceToDb(userId, balance);

        int result =  userBalanceRepository.minusBalanceIfSufficient(userId, price);
        UserBalance userBalance = userBalanceRepository.findById(userId).orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(userBalance.getBalance()).isEqualTo(expectedPrice);
    }

    @Test
    void plusBalance_shouldReturnOneAndSetUserBalancePlusPrice() {
        long userId = 1L;
        BigDecimal balance = new BigDecimal("123.32");
        BigDecimal price =  new BigDecimal("22.33");
        BigDecimal expectedPrice = balance.add(price);
        addUserAndHisBalanceToDb(userId, balance);

        int result = userBalanceRepository.plusBalance(userId, price);
        UserBalance userBalance = userBalanceRepository.findById(userId).orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(userBalance.getBalance()).isEqualTo(expectedPrice);
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
