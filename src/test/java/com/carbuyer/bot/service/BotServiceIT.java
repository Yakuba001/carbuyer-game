package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotAddedStatus;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

public class BotServiceIT extends BaseIntegrationTest {

    @SpyBean
    private BotService botService;
    @Autowired BotDistributeService distributeService;
    @Autowired private UserFacade userFacade;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private BotConfigRepository botConfigRepository;

    @Value("${app.bot.initial-size:0}")
    private int START_FIX_BOTS_AMOUNT;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE bot_config CASCADE");
        jdbcTemplate.update("INSERT INTO bot_config (id, active_bots) VALUES (?, ?)", 1, 0);
    }

    @Test
    void botAdderManager_generalHappyPathTest() {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        List<StartBotResponse> resultResponseList = new ArrayList<>(threadCount);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        startLatch.await();
                        resultResponseList.add(distributeService.botAdderManager());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }
            speedExecutionChecker(startLatch, endLatch);
            StartBotResponse finalStatus = distributeService.botAdderManager();
            boolean oneTimeAdded = userFacade.existsById((long) START_FIX_BOTS_AMOUNT);
            boolean doubleTimeAdded = userFacade.existsById((long) START_FIX_BOTS_AMOUNT + 1);

            assertThat(resultResponseList.size()).isEqualTo(threadCount);
            assertThat(finalStatus.status()).isEqualTo(BotAddedStatus.BOTS_ADDED.toString());
            assertThat(oneTimeAdded).isTrue();
            assertThat(doubleTimeAdded).isFalse();
        }
    }

    @Test
    void botAdderManager_shouldRollbackStatusToNotAdded_whenAddBotsThrowsException() {
        doThrow(new RuntimeException("Artificial error in cycle"))
                .when(botService).addBots();

        assertThatThrownBy(() -> distributeService.botAdderManager()).isInstanceOf(BotCreationException.class);

        ConfigStatus finalStatus = ConfigStatus.valueOf(botConfigRepository.getConfigStatus());
        assertThat(finalStatus).isEqualTo(ConfigStatus.NOT_ADDED);
    }

    private void speedExecutionChecker(CountDownLatch startLatch, CountDownLatch endLatch) {
        long startTime = System.nanoTime();

        try {
            startLatch.countDown();
            endLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;
        System.out.println("Execution time: " + (durationMs / 1000.0) + " seconds");
    }
}
