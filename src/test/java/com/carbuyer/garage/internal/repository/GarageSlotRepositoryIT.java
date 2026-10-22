package com.carbuyer.garage.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class GarageSlotRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private GarageSlotRepository garageSlotRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager em;

    private static final long TEST_USER_ID = 1L;
    private static final int TEST_SLOT_NUMBER = 1;
    private static final UUID TEST_CAR_ID = UUID.randomUUID();

    @ParameterizedTest
    @MethodSource("objWithNullElements")
    void checkNotNullConstraints_shouldThrowException_whenInputFieldsAreNull(GarageSlot garageSlot) {
        assertThatThrownBy(() -> garageSlotRepository.saveAndFlush(garageSlot))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkSetDefaultAmounts_shouldSetDefaultAmounts_whenUserNotSendIt() {
        addUserToDb();
        addCarToDb();
        jdbcTemplate.update("INSERT INTO garage_slots (user_id, slot_number, car_id) VALUES (?, ?, ?)",
                TEST_USER_ID, TEST_SLOT_NUMBER, TEST_CAR_ID);

        GarageSlot result = garageSlotRepository.findByUserIdAndCarId(TEST_USER_ID, TEST_CAR_ID).orElseThrow();

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GarageSlotStatus.EMPTY);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void checkCascadeDeleted_shouldCascadeDeleted_whenUserIsDeleted() {
        addUserToDb();
        GarageSlot exampleSlot = new GarageSlot(TEST_USER_ID, TEST_SLOT_NUMBER, null, GarageSlotStatus.EMPTY);
        garageSlotRepository.saveAndFlush(exampleSlot);

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", TEST_USER_ID);

        Optional<GarageSlot> result = garageSlotRepository
                .findFirstByUserIdAndStatus(TEST_USER_ID, GarageSlotStatus.EMPTY);

        assertThat(result).isEmpty();
    }

    @Test
    void checkOnCarDeleteSetNull_shouldSetNullInCarField_whenCarWasBeenDeleted() {
        addUserCarAndTestGarageToDb();

        jdbcTemplate.update("DELETE FROM cars WHERE id = ?", TEST_CAR_ID);
        em.clear();

        GarageSlot result = garageSlotRepository.findFirstByUserIdAndStatus(TEST_USER_ID, GarageSlotStatus.EMPTY)
                .orElseThrow();

        assertThat(result.getCarId()).isNull();
    }

    @Test
    void checkConstraintUniqueSlotNumberByUserId_shouldThrowException_whenUserGetUnUniqueSlotNumber() {
        addUserCarAndTestGarageToDb();
        GarageSlot exampleSlot = new GarageSlot(TEST_USER_ID, TEST_SLOT_NUMBER, TEST_CAR_ID, GarageSlotStatus.EMPTY);

        assertThatThrownBy(() -> garageSlotRepository.saveAndFlush(exampleSlot))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraintGarageSlotStatus_shouldThrowException_whenGarageSlotWithIncorrectStatus() {
        addUserCarAndTestGarageToDb();

        assertThatThrownBy(() -> jdbcTemplate.update("INSERT INTO garage_slots " +
                "(user_id, slot_number, car_id, status) VALUES (?, ?, ?, ?)",
                TEST_USER_ID, TEST_SLOT_NUMBER, TEST_CAR_ID, "Invalid")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findFirstByUserIdAndStatus_shouldReturnCorrectGarageSlot_whenSlotIsExists() {
        addUserCarAndTestGarageToDb();

        Optional<GarageSlot> result = garageSlotRepository
                .findFirstByUserIdAndStatus(TEST_USER_ID, GarageSlotStatus.EMPTY);

        assertThat(result).isPresent();
    }

    @Test
    void findFirstByUserIdAndStatus_shouldReturnEmptyOptional_whenSlotDoesNotExist() {
        Optional<GarageSlot> result =
                garageSlotRepository.findFirstByUserIdAndStatus(TEST_USER_ID, GarageSlotStatus.EMPTY);

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndCarId_shouldReturnCorrectGarageSlot_whenSlotIsExists() {
        addUserCarAndTestGarageToDb();

        Optional<GarageSlot> result = garageSlotRepository.findByUserIdAndCarId(TEST_USER_ID, TEST_CAR_ID);

        assertThat(result).isPresent();
    }

    @Test
    void findByUserIdAndCarId_shouldReturnEmptyOptional_whenSlotIsNotExists() {
        Optional<GarageSlot> result = garageSlotRepository.findByUserIdAndCarId(TEST_USER_ID, TEST_CAR_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void uniqueSlotsCar_shouldThrowException_whenAssigningSameCarToTwoSlots() {
        addUserCarAndTestGarageToDb();
        long anotherUserId = 2L;
        String username = "Joe";
        String email = "joe@g.c";
        String password = "123GreatJoe";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                anotherUserId, username, email, password);
        GarageSlot slotWithSameCarId =
                new GarageSlot(anotherUserId, 2, TEST_CAR_ID, GarageSlotStatus.EMPTY);

        assertThatThrownBy(() -> garageSlotRepository.saveAndFlush(slotWithSameCarId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueSlotsCar_shouldWorkingCorrectly_whenTheSameUserHasMoreThanOneSlotWithNullCarId() {
        addUserCarAndTestGarageToDb();
        GarageSlot nullCarIdOne = new GarageSlot(TEST_USER_ID, 2, null, GarageSlotStatus.EMPTY);
        GarageSlot nullCarIdTwo = new GarageSlot(TEST_USER_ID, 3, null, GarageSlotStatus.EMPTY);
        garageSlotRepository.saveAllAndFlush(List.of(nullCarIdOne, nullCarIdTwo));
        long expectedResult = 3L;

        long result = garageSlotRepository.count();

        assertThat(result).isEqualTo(expectedResult);
    }

    private static Stream<GarageSlot> objWithNullElements() {
        return Stream.of(
                new GarageSlot(null, TEST_SLOT_NUMBER, TEST_CAR_ID, GarageSlotStatus.EMPTY),
                new GarageSlot(TEST_USER_ID, null, TEST_CAR_ID, GarageSlotStatus.EMPTY),
                new GarageSlot(TEST_USER_ID, TEST_SLOT_NUMBER, TEST_CAR_ID, null)
        );
    }

    private void addUserToDb() {
        String username = "Bill";
        String email = "bill@g.c";
        String password = "123GreatBill";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                TEST_USER_ID, username, email, password);
    }

    private void addCarToDb() {
        String randomVin1 = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int conditionScore = 85;
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, condition_score, owner_id) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?, ?)",
                TEST_CAR_ID, randomVin1, model, conditionScore, TEST_USER_ID);
    }

    private void addUserCarAndTestGarageToDb() {
        addUserToDb();
        addCarToDb();
        GarageSlot exampleSlot = new GarageSlot(TEST_USER_ID, TEST_SLOT_NUMBER, TEST_CAR_ID, GarageSlotStatus.EMPTY);
        garageSlotRepository.saveAndFlush(exampleSlot);
    }
}
