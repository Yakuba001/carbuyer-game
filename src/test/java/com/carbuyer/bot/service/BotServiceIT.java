package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotAddedStatus;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.exception.exceptions.BotCreationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

public class BotServiceIT extends BaseIntegrationTest {

    @SpyBean
    private BotService botService;
    @Autowired
    BotDistributeService distributeService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BotConfigRepository botConfigRepository;

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
        List<StartBotResponse> resultResponseList = new CopyOnWriteArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
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
            try {
                startLatch.countDown();
                endLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            StartBotResponse finalStatus = distributeService.botAdderManager();

            assertThat(resultResponseList.size()).isEqualTo(threadCount);
            assertThat(finalStatus.status()).isEqualTo(BotAddedStatus.BOTS_ADDED.toString());
        }
    }

    @Test
    void addBots_oneThreadHappyPathTest() {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.execute(() -> {
                try {
                    startLatch.await();
                    long startTime = System.nanoTime();
                    botService.addBots();
                    long endTime = System.nanoTime();
                    long durationMs = (endTime - startTime) / 1000000;
                    System.out.println("Execution time: " + (durationMs / 1000.0) + " seconds");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
            startLatch.countDown();
            try {
                endLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
}
