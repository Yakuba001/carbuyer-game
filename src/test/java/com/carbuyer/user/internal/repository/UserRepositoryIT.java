package com.carbuyer.user.internal.repository;

import com.carbuyer.containers.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class UserRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_USERNAME = "User";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_EMAIL = "email@g.com";

    @ParameterizedTest
    @MethodSource(value = "duplicateCheckerUsers")
    void checkUniqueNameEmailFields_shouldThrowDataViolationException_whenFieldsUnUnique(User user) {
        addUserToDb("test", "test");

        assertThatThrownBy(() -> userRepository.save(user)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @MethodSource(value = "nullCheckerUsers")
    void checkNotNullConstraints_shouldThrowDataViolationException_whenFieldsAreNull(User user) {
        assertThatThrownBy(() -> userRepository.save(user)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkDefaultsAmounts_shouldSetIdMaxSlotsVersionCreatedAt_whenUserAdded() {
        User user = new User(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getMaxSlots()).isEqualTo(1);
        assertThat(savedUser.getVersion()).isEqualTo(0);
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenUserWithThisEmailExists() {
        addUserToDb(TEST_USERNAME, TEST_EMAIL);

        boolean result = userRepository.existsByEmail(TEST_EMAIL);

        assertThat(result).isTrue();
    }

    @Test
    void existsByName_shouldReturnTrue_whenUserWithThisNameExists() {
        addUserToDb(TEST_USERNAME, TEST_EMAIL);

        boolean result = userRepository.existsByName(TEST_USERNAME);

        assertThat(result).isTrue();
    }

    @Test
    void findByEmail_shouldReturnExistingUser() {
        addUserToDb(TEST_USERNAME, TEST_EMAIL);

        Optional<User> user = userRepository.findByEmail(TEST_EMAIL);

        assertThat(user.isPresent()).isTrue();
    }

    @Test
    void findByName_shouldReturnExistingUser() {
        addUserToDb(TEST_USERNAME, TEST_EMAIL);

        Optional<User> user = userRepository.findByName(TEST_USERNAME);

        assertThat(user.isPresent()).isTrue();
    }

    @Test
    void countByIsBotTrueAndIsBotActiveTrue_shouldReturnAmountOfActiveBots_whenThisBotsExistsInDb() {
        User savedUser = new User(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        savedUser.setBot(true);
        savedUser.setBotActive(true);
        addUserToDb("Bill", "cosby@gmail.com");
        userRepository.save(savedUser);

        long result = userRepository.countByIsBotTrueAndIsBotActiveTrue();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void countByIsBotTrueAndIsBotActiveFalse_shouldReturnAmountOfInActiveBots_whenThisBotsExistsInDb() {
        User savedUser = new User(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        savedUser.setBot(true);
        addUserToDb("Bill", "cosby@gmail.com");
        userRepository.save(savedUser);

        long result = userRepository.countByIsBotTrueAndIsBotActiveFalse();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void activateBots_shouldSwitchIsBotActiveFromFalseToTrue_whenAmountOfBotsWithActivateFalseExistsInDb() {
        int amount = 1;
        User savedUser = new User(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        savedUser.setBot(true);
        userRepository.save(savedUser);

        userRepository.activateBots(amount);
        entityManager.flush();
        entityManager.clear();

        User resultUser = userRepository.findByName(TEST_USERNAME).orElse(null);
        assertThat(resultUser).isNotNull();

        assertThat(resultUser.isBotActive()).isTrue();
    }

    @Test
    void deactivateBots_shouldSwitchIsBotActiveFromFalseToTrue_whenAmountOfBotsWithDeactivateFalseExistsInDb() {
        int amount = 1;
        User savedUser = new User(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        savedUser.setBot(true);
        savedUser.setBotActive(true);
        userRepository.save(savedUser);

        userRepository.deactivateBots(amount);
        entityManager.flush();
        entityManager.clear();

        User resultUser = userRepository.findByName(TEST_USERNAME).orElse(null);
        assertThat(resultUser).isNotNull();

        assertThat(resultUser.isBotActive()).isFalse();
    }

    @Test
    void findActiveBotIdsWithLimit_shouldReturnActiveBotIdsWithLimit_whenBotsExistsInDb() {
        int activeBotsWithEmptySlotAmount = 10;
        int limit = 5;
        activeBotsAdder(activeBotsWithEmptySlotAmount);

        List<Long> activeBotIds = userRepository.findActiveBotIdsWithLimit(limit);

        assertThat(activeBotIds.size()).isEqualTo(limit);
    }

    @Test
    void findActiveBotIdsWithLimit_shouldReturnAllActiveBotIds_whenActiveBotsLessThanLimit() {
        int activeBotsWithEmptySlotAmount = 10;
        int botWithOccupiedSlot = 1;
        int expectedAmount = activeBotsWithEmptySlotAmount - botWithOccupiedSlot;
        int limit = 50;
        activeBotsAdder(activeBotsWithEmptySlotAmount);

        List<Long> activeBotIds = userRepository.findActiveBotIdsWithLimit(limit);

        assertThat(activeBotIds.size()).isEqualTo(expectedAmount);
    }

    private void addUserToDb(String name, String email) {
        User savedUser = new User(name, email, TEST_PASSWORD);
        userRepository.save(savedUser);
    }

    private static Stream<User> nullCheckerUsers() {
        return Stream.of(
                new User(null, "test1", "test2"),
                new User("test", null, "test2"),
                new User("test", "test1", null)
        );
    }

    private static Stream<User> duplicateCheckerUsers() {
        return Stream.of(
                new User("test", "test1", "test1"),
                new User("test1", "test", "test1")
        );
    }

    private void activeBotsAdder(int amount) {
        List<User> activeBots = new ArrayList<>();
        String emptyStatus = "EMPTY";
        String occupiedStatus = "OCCUPIED";
        for (int i = 0; i < amount; i++) {
            User savedUser = new User(TEST_USERNAME + i, TEST_EMAIL + i, TEST_PASSWORD);
            savedUser.setBot(true);
            savedUser.setBotActive(true);
            activeBots.add(savedUser);
        }
        List<User> savedUsers = userRepository.saveAllAndFlush(activeBots);
        long firstId = savedUsers.getFirst().getId();
        garageAdder(firstId, occupiedStatus);
        for (int i = 1; i < amount; i++) {
            garageAdder(savedUsers.get(i).getId(), emptyStatus);
        }
    }

    private void garageAdder(long userId, String status) {
        int slotNumber = 1;
        jdbcTemplate.update("INSERT INTO garage_slots (user_id, slot_number, status) " +
                "VALUES (?, ?, ?)", userId, slotNumber, status);
    }
}
