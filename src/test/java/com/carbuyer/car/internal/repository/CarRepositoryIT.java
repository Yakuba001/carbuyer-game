package com.carbuyer.car.internal.repository;

import com.carbuyer.containers.DataJpaIntegrationTest;
import com.carbuyer.car.internal.entity.Car;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class CarRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private CarRepository carRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void checkUniqueField_shouldThrowException_whenVinUnunique() {
        long userId = 1L;
        addUserToDb(userId);
        String randomVin1 = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int mileage = 10000;
        int conditionScore = 85;
        BigDecimal price = new BigDecimal("123.21");
        Car car = new Car(randomVin1, model, mileage, conditionScore, price, userId);
        Car duplicateVin = new Car(randomVin1, model, mileage, conditionScore, price, userId);
        carRepository.saveAndFlush(car);

        assertThatThrownBy(() -> carRepository.saveAndFlush(duplicateVin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraintWhenFieldCanBeOnlyPositive_shouldThrowException_whenMileageLowerThanZero() {
        long userId = 1L;
        addUserToDb(userId);
        addTestCorrectCarToDb(userId);
        int negativeMileage = -1;

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE cars SET mileage = ? WHERE owner_id = ?",
                negativeMileage, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraintWhenFieldCanBeOnlyPositive_shouldThrowException_whenRealPriceLowerThanZero() {
        long userId = 1L;
        addUserToDb(userId);
        addTestCorrectCarToDb(userId);
        BigDecimal negativePrice = new BigDecimal("-1.00");

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE cars SET real_price = ? WHERE owner_id = ?",
                negativePrice, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void checkConstraint_shouldThrownException_whenConditionScoreNotFrom0To100Borders(int negativeConditionScore) {
        long userId = 1L;
        addUserToDb(userId);
        addTestCorrectCarToDb(userId);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE cars SET condition_score = ? WHERE owner_id = ?",
                negativeConditionScore, userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraintToDeleteUser_shouldThrowException_whenUserHasACar() {
        long userId = 1L;
        addUserToDb(userId);
        addTestCorrectCarToDb(userId);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkDefaultValues_shouldSetDefaultValues_whenUserDidNotSendAmounts() {
        long userId = 1L;
        addUserToDb(userId);
        String randomVin1 = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int conditionScore = 85;
        jdbcTemplate.update("INSERT INTO cars (vin, model, condition_score, owner_id) VALUES (?, ?, ?, ?)",
                randomVin1, model, conditionScore, userId);

        List<Car> saveCarList = carRepository.findAll();
        Car savedCar = saveCarList.stream().findFirst().orElseThrow();

        assertThat(savedCar.getId()).isNotNull();
        assertThat(savedCar.getMileage()).isEqualTo(0);
        assertThat(savedCar.getRealPrice()).isEqualTo("0.00");
        assertThat(savedCar.getCreatedAt()).isNotNull();
    }

    @Test
    void setNewCarOwner_shouldReturnZero_whenCarCantChangedHisOwner() {
        long userId = 1L;
        long anotherOwnerId = 2L;
        long buyerId = 3L;
        addUserToDb(anotherOwnerId);
        UUID carId = addTestCorrectCarToDb(anotherOwnerId);

        int result = carRepository.setNewCarOwner(userId, buyerId, carId);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void setNewCarOwner_shouldReturnOne_whenCarChangedHisOwner() {
        long ownerId = 1L;
        addUserToDb(ownerId);
        long buyerId = 2L;
        String username = "John";
        String userEmail = "bigJohn@g.com";
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                buyerId, username, userEmail, userPassword);
        UUID carId = addTestCorrectCarToDb(ownerId);
        Car exampleCar = carRepository.findById(carId).orElseThrow();
        long currentOwnerId = exampleCar.getOwnerId();

        int result = carRepository.setNewCarOwner(ownerId, buyerId, carId);

        long newOwnerId = carRepository.findById(carId).orElseThrow().getOwnerId();
        assertThat(result).isEqualTo(1);
        assertThat(currentOwnerId).isEqualTo(ownerId);
        assertThat(newOwnerId).isEqualTo(buyerId);
    }

    @Test
    void findForUpdateById_shouldReturnCar_whenCarExists() {
        long userId = 1L;
        addUserToDb(userId);
        UUID carId = addTestCorrectCarToDb(userId);

        Car savedCar = carRepository.findForUpdateById(carId).orElseThrow();

        assertThat(savedCar.getId()).isEqualTo(carId);
    }

    @Test
    void findForUpdateById_shouldReturnOptionalEmpty_whenCarDoesNotExist() {
        UUID carId = UUID.randomUUID();

        Optional<Car> result = carRepository.findForUpdateById(carId);

        assertThat(result).isEmpty();
    }

    private UUID addTestCorrectCarToDb(long userId) {
        String randomVin1 = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String model = "Opel";
        int mileage = 10000;
        int conditionScore = 85;
        BigDecimal price = new BigDecimal("123.21");
        Car car = new Car(randomVin1, model, mileage, conditionScore, price, userId);
        carRepository.saveAndFlush(car);
        return car.getId();
    }

    private void addUserToDb(long userId) {
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                userId, username, userEmail, userPassword);
    }
}
