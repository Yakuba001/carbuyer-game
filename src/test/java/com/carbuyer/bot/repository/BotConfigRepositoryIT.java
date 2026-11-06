package com.carbuyer.bot.repository;

import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.containers.DataJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BotConfigRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private BotConfigRepository botConfigRepository;

    @Test
    @Transactional
    void updateStatusIfExpected_shouldReturnOne_whenDatabaseIsUpdated() {
        ConfigStatus currStatus = ConfigStatus.NOT_ADDED;
        ConfigStatus newStatus = ConfigStatus.ADDED;

        int result = botConfigRepository.updateStatusIfExpected(newStatus, currStatus);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @Transactional
    void updateStatusIfExpected_shouldReturnZero_whenDatabaseIsNotUpdated() {
        ConfigStatus currStatus = ConfigStatus.NOT_ADDED;
        ConfigStatus newStatus = ConfigStatus.ADDED;

        int result = botConfigRepository.updateStatusIfExpected(currStatus, newStatus);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void getCurrentStatus_shouldReturnCurrentStatus() {
        String expectedStatus = "NOT_ADDED";

        String result = botConfigRepository.getConfigStatus();

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    void singleRowInDb_shouldThrowException_whenInDbAlreadyExistsRow() {
        BotConfig config = new BotConfig();
        ReflectionTestUtils.setField(config, "id", 2);

        assertThatThrownBy(() -> botConfigRepository.saveAndFlush(config))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkCorrectInputStatusInConfig_shouldThrowException_whenStatusNull() {
        BotConfig config = new BotConfig();
        config.setStatus(null);

        assertThatThrownBy(() -> botConfigRepository.saveAndFlush(config))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
