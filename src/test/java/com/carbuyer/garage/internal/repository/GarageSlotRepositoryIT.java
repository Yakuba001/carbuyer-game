package com.carbuyer.garage.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.entity.GarageSlotStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class GarageSlotRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private GarageSlotRepository garageSlotRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void checkConstraint_oneUserCanHaveOnlyUniqueGarageSlotsNumber() {
        long userId = 1L;
        String username = "Bob";
        String userEmail = "bob@g.com";
        String userPassword = "bobby123";
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)",
                userId, username, userEmail, userPassword);
        GarageSlot first = new GarageSlot(userId, 1, null, GarageSlotStatus.EMPTY);
        GarageSlot second = new GarageSlot(userId, 2, null, GarageSlotStatus.EMPTY);
        GarageSlot thirdWithTheSameSlotNumber = new GarageSlot(userId, 1, null, GarageSlotStatus.EMPTY);

        garageSlotRepository.save(first);
        garageSlotRepository.save(second);

        assertThatThrownBy(() -> garageSlotRepository.saveAndFlush(thirdWithTheSameSlotNumber))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
