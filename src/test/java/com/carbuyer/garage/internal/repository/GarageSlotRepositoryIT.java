package com.carbuyer.garage.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

    @Test
    void checkConstraint_oneUserCanHaveOnlyUniqueGarageSlotsNumber() {
        long userId = 1L;
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        addUserToDb(userId, username, userEmail, userPassword);
        GarageSlot first = new GarageSlot(userId, 1, null, GarageSlotStatus.EMPTY);
        GarageSlot second = new GarageSlot(userId, 2, null, GarageSlotStatus.EMPTY);
        GarageSlot thirdWithTheSameSlotNumber = new GarageSlot(userId, 1, null, GarageSlotStatus.EMPTY);

        garageSlotRepository.save(first);
        garageSlotRepository.save(second);

        assertThatThrownBy(() -> garageSlotRepository.saveAndFlush(thirdWithTheSameSlotNumber))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKey_shouldSetCarIdToNullOnCarDelete() {
        long userId = 1L;
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        addUserToDb(userId, username, userEmail, userPassword);
        UUID firstCarId = UUID.randomUUID();
        String randomVin1 = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        jdbcTemplate.update("INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                firstCarId, randomVin1, "Test Model", 10000, 85, 15000.00, userId);
        GarageSlot slot = new GarageSlot(userId, 1, firstCarId, GarageSlotStatus.OCCUPIED);

        garageSlotRepository.saveAndFlush(slot);

        jdbcTemplate.update("DELETE FROM cars WHERE id = ?", firstCarId);

        em.clear();

        GarageSlot updatedSlot = garageSlotRepository.findById(slot.getId()).orElseThrow();

        assertThat(updatedSlot.getCarId()).isNull();
    }

    @Test
    void garageStatusConstraint_shouldThrowDataIntegrityViolationException_whenStatusIsNotEmptyOrOccupied() {
        long userId = 1L;
        int slotNumber = 1;
        GarageSlotStatus status = GarageSlotStatus.EMPTY;
        addUserAndGarageSlotToDb(userId, slotNumber, status);

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE garage_slots SET status = 'BROKEN' " +
                        "WHERE user_id = ? AND slot_number = ?",
                userId, slotNumber)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @EnumSource(value = GarageSlotStatus.class, names = {"EMPTY", "OCCUPIED"})
    void existByUserIdAndStatus_shouldReturnTrue_whenUserHasGarageSlotWithStatus(GarageSlotStatus status) {
        long userId = 1L;
        int slotNumber = 1;
        addUserAndGarageSlotToDb(userId, slotNumber, status);

        boolean result = garageSlotRepository.existsByUserIdAndStatus(userId, status);

        assertThat(result).isTrue();
    }

    @Test
    void existByUserIdAndStatus_shouldReturnFalse_whenUserHasNoGarageSlotWithStatus() {
        long userId = 1L;
        int slotNumber = 1;
        GarageSlotStatus status = GarageSlotStatus.OCCUPIED;
        addUserAndGarageSlotToDb(userId, slotNumber, status);

        boolean result = garageSlotRepository.existsByUserIdAndStatus(userId, GarageSlotStatus.EMPTY);

        assertThat(result).isFalse();
    }

    private void addUserAndGarageSlotToDb(long userId, Integer slotNumber, GarageSlotStatus status) {
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        addUserToDb(userId, username, userEmail, userPassword);
        GarageSlot slot = new GarageSlot(userId, slotNumber, null, status);
        garageSlotRepository.saveAndFlush(slot);
    }

    private void addUserToDb(long userId, String username, String email, String password) {
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                userId, username, email, password);
    }
}
