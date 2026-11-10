package com.carbuyer.user.internal.repository;

import com.carbuyer.containers.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class UserRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

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
    void findActiveSellerBotIds_shouldReturnAllActiveSellerBotIds_whenSuchExistsInDb() {
        int amount = 100;
        addBotsAndListingsInDb(amount);

        List<Long> resultIdsList = userRepository.findActiveSellerBotIds();

        assertThat(resultIdsList.size()).isEqualTo(amount / 2);
    }

    @Test
    void findBuyerBotIds_shouldReturnAllBuyerBotIds_whenSuchExistsInDb() {
        int amount = 100;
        addBotsAndListingsInDb(amount);

        List<Long> resultIdsList = userRepository.findBuyerBotIds();

        assertThat(resultIdsList.size()).isEqualTo(amount / 2);
    }

    private void addBotsAndListingsInDb(int amount) {
        List<User> bots = new ArrayList<>(amount);
        List<UUID> carIds = new ArrayList<>(amount / 2);
        for (int i = 0; i < amount; i++) {
            User user = new User(TEST_USERNAME + i, TEST_EMAIL + i, TEST_PASSWORD);
            user.setBot(true);
            bots.add(user);
            if (i % 2 == 0) {
                carIds.add(UUID.randomUUID());
            }
        }
        List<User> existBotsList = userRepository.saveAllAndFlush(bots);
        List<Long> sellerIds = existBotsList.subList(0, existBotsList.size() / 2).stream().map(User::getId).toList();

        String carSql = "INSERT INTO cars (id, vin, model, condition_score, owner_id) " +
                "OVERRIDING SYSTEM VALUE VALUES (:carId, :vin, :model, :condition_score, :ownerId)";
        MapSqlParameterSource[] carParams = IntStream.range(0, carIds.size())
                .mapToObj(idx -> {
                    MapSqlParameterSource param = new MapSqlParameterSource();
                    param.addValue("carId", carIds.get(idx));
                    param.addValue("vin", vinGenerator());
                    param.addValue("model", "Opel");
                    param.addValue("condition_score", 10);
                    param.addValue("ownerId", sellerIds.get(idx));
                    return param;
                }).toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(carSql, carParams);

        String marketSql = "INSERT INTO market_listings (car_id, seller_id) VALUES (:carId, :sellerId)";
        MapSqlParameterSource[] params = IntStream.range(0, sellerIds.size())
                .mapToObj(idx -> {
                    MapSqlParameterSource param = new MapSqlParameterSource();
                    param.addValue("carId", carIds.get(idx));
                    param.addValue("sellerId", sellerIds.get(idx));
                    return param;
                }).toArray(MapSqlParameterSource[]::new);
        jdbcTemplate.batchUpdate(marketSql, params);
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

    private String vinGenerator() {
        return RandomStringUtils.randomAlphanumeric(10);
    }
}
