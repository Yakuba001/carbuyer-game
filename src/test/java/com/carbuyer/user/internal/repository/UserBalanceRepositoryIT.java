package com.carbuyer.user.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.UserBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void checkConstraintNotNullBalance_shouldThrowException_whenBalanceIsNull() {
        long userId = 1L;
        addUserToDb(userId);

        assertThatThrownBy(() -> jdbcTemplate
                .update("INSERT INTO user_balances (user_id, balance) OVERRIDING SYSTEM VALUE VALUES (?)",
                        new Object[]{userId, null}, new int[]{Types.BIGINT, Types.NUMERIC}))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkDefaultBalance_shouldSetDefaultBalance_whenBalanceNotSend() {
        long userId = 1L;
        addUserToDb(userId);
        jdbcTemplate.update("INSERT INTO user_balances (user_id) OVERRIDING SYSTEM VALUE VALUES (?)", userId);

        UserBalance result = userBalanceRepository.findById(userId).orElseThrow();

        assertThat(result.getBalance()).isEqualTo("0.00");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getVersion()).isEqualTo(0);
    }

    @Test
    void updateBalance_shouldReturnOne_whenUserBalanceChanged() {
        long userId = 1L;
        BigDecimal buyerBalance = new BigDecimal("228.21");
        BigDecimal newBuyerBalance = new BigDecimal("118.22");
        addUserAndHisBalanceToDb(userId, buyerBalance);

        int result = userBalanceRepository.updateBalance(userId, newBuyerBalance);

        UserBalance buyerBal = userBalanceRepository.findById(userId).orElseThrow();

        assertThat(result).isEqualTo(1);
        assertThat(buyerBal.getBalance()).isEqualTo(newBuyerBalance);
    }

    @Test
    void findByUserId_shouldReturnUserBalance_whenUserWithBalanceExists() {
        long userId = 1L;
        BigDecimal buyerBalance = new BigDecimal("228.21");
        addUserAndHisBalanceToDb(userId, buyerBalance);

        UserBalance savedBalance = userBalanceRepository.findByUserId(userId).orElseThrow();

        assertThat(savedBalance.getBalance()).isEqualTo(buyerBalance);
        assertThat(savedBalance.getUserId()).isEqualTo(userId);
    }

    @Test
    void findByUserId_shouldReturnOptionalEmpty_whenUserOrBalanceNotExists() {
        Optional<UserBalance> result = userBalanceRepository.findByUserId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void updateBalance_shouldReturnZero_whenUserBalanceNotChanged() {
        long userId = 1L;
        BigDecimal buyerBalance = new BigDecimal("228.21");

        int result = userBalanceRepository.updateBalance(userId, buyerBalance);

        assertThat(result).isEqualTo(0);
    }

    private void addUserAndHisBalanceToDb(long userId, BigDecimal balance) {
        addUserToDb(userId);
        jdbcTemplate.update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", userId, balance);
    }

    private void addUserToDb(long userId) {
        String username = "Bill";
        String email = "billGreat@g.com";
        String password = "password";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                userId, username, email, password);
    }
}
