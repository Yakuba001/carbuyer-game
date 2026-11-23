package com.carbuyer.car.internal.repository;

import com.carbuyer.containers.DataJpaIntegrationTest;
import com.carbuyer.car.internal.entity.Car;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
public class CarRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private CarRepository carRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long TEST_USER_ID = 1L;
    private static final String TEST_VIN =
            UUID.randomUUID().toString().replace("-", "").substring(0, 17);
    private static final String TEST_MODEL = "Opel";
    private static final int TEST_MILEAGE = 10_000;
    private static final int TEST_CONDITION_SCORE = 85;
    private static final String NAME_BOB = "Bob";
    private static final String EMAIL_BOB = "bob@gmail.com";

    @BeforeEach
    public void setup() {
        addUserToDb(TEST_USER_ID, NAME_BOB, EMAIL_BOB, false);
    }

    @Test
    void checkUniqueField_shouldThrowException_whenVinUnunique() {
        Car car = new Car(TEST_VIN, TEST_MODEL, TEST_MILEAGE, TEST_CONDITION_SCORE, TEST_USER_ID);
        Car duplicateVin = new Car(TEST_VIN, TEST_MODEL, TEST_MILEAGE, TEST_CONDITION_SCORE, TEST_USER_ID);
        carRepository.saveAndFlush(car);

        assertThrows(DataIntegrityViolationException.class, () -> carRepository.saveAndFlush(duplicateVin));
    }

    @Test
    void checkConstraintWhenFieldCanBeOnlyPositive_shouldThrowException_whenMileageLowerThanZero() {
        addTestCorrectCarToDb(TEST_USER_ID, TEST_VIN);
        int negativeMileage = -1;

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("UPDATE cars SET mileage = ? WHERE owner_id = ?",
                negativeMileage, TEST_USER_ID));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void checkConstraint_shouldThrownException_whenConditionScoreNotFrom0To100Borders(int negativeConditionScore) {
        addTestCorrectCarToDb(TEST_USER_ID, TEST_VIN);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("UPDATE cars SET condition_score = ? WHERE owner_id = ?",
                negativeConditionScore, TEST_USER_ID));
    }

    @Test
    void checkConstraintToDeleteUser_shouldThrowException_whenUserHasACar() {
        addTestCorrectCarToDb(TEST_USER_ID, TEST_VIN);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", TEST_USER_ID));
    }

    @Test
    void checkDefaultValues_shouldSetDefaultValues_whenUserDidNotSendAmounts() {
        UUID carId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, condition_score, owner_id) VALUES (?, ?, ?, ?, ?)",
                carId, TEST_VIN, TEST_MODEL, TEST_CONDITION_SCORE, TEST_USER_ID);

        Car savedCar = carRepository.findById(carId).orElse(null);

        assertThat(savedCar).isNotNull();
        assertThat(savedCar.getId()).isNotNull();
        assertThat(savedCar.getMileage()).isEqualTo(0);
        assertThat(savedCar.getCreatedAt()).isNotNull();
    }

    @Test
    void setNewCarOwner_shouldReturnZero_whenCarCantChangedHisOwner() {
        long anotherOwnerId = 2L;
        long buyerId = 3L;
        addUserToDb(anotherOwnerId, "John", "john@gmail.com", false);
        UUID carId = addTestCorrectCarToDb(anotherOwnerId, TEST_VIN);

        int result = carRepository.setNewCarOwner(TEST_USER_ID, buyerId, carId);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void setNewCarOwner_shouldReturnOne_whenCarChangedHisOwner() {
        long buyerId = 2L;
        addUserToDb(buyerId, "Arnold", "bigBoy@gmail.com", false);
        UUID carId = addTestCorrectCarToDb(TEST_USER_ID, TEST_VIN);
        Car exampleCar = carRepository.findById(carId).orElseThrow();
        long currentOwnerId = exampleCar.getOwnerId();

        int result = carRepository.setNewCarOwner(TEST_USER_ID, buyerId, carId);

        long newOwnerId = carRepository.findById(carId).orElseThrow().getOwnerId();

        assertThat(result).isEqualTo(1);
        assertThat(currentOwnerId).isEqualTo(TEST_USER_ID);
        assertThat(newOwnerId).isEqualTo(buyerId);
    }

    @Test
    void findForUpdateById_shouldReturnCar_whenCarExists() {
        UUID carId = addTestCorrectCarToDb(TEST_USER_ID, TEST_VIN);

        Car savedCar = carRepository.findForUpdateById(carId).orElseThrow();

        assertThat(savedCar.getId()).isEqualTo(carId);
    }

    @Test
    void findForUpdateById_shouldReturnOptionalEmpty_whenCarDoesNotExist() {
        UUID carId = UUID.randomUUID();

        Optional<Car> result = carRepository.findForUpdateById(carId);

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByOwnerIdIn_shouldDeletedCarsInDbByOwnerIds_whenCarWithThisOwnerIdsExistsAndUserIsBot() {
        long secondUserId = 2L;
        String secondCarVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        List<Long> ownerIds = List.of(TEST_USER_ID, secondUserId);
        addUserToDb(secondUserId, "Emi", "nem@gmail.com", true);
        addTestCorrectCarToDb(TEST_USER_ID, TEST_VIN);
        addTestCorrectCarToDb(secondUserId, secondCarVin);

        long currCarsCount = carRepository.count();

        carRepository.deleteByOwnerIdIn(ownerIds);

        long resultCarsCount = carRepository.count();

        assertThat(currCarsCount).isEqualTo(ownerIds.size());
        assertThat(resultCarsCount).isEqualTo(1);
    }

    private UUID addTestCorrectCarToDb(long userId, String vin) {
        Car car = new Car(vin, TEST_MODEL, TEST_MILEAGE, TEST_CONDITION_SCORE, userId);
        carRepository.saveAndFlush(car);
        return car.getId();
    }

    private void addUserToDb(long userId, String name, String email, boolean isBot) {
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash, is_bot) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?, ?)",
                userId, name, email, userPassword, isBot);
    }
}
