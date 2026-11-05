package com.carbuyer.bot.service;

import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Import(BotServiceIT.BotServiceITComponent.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BotServiceIT extends BaseIntegrationTest {

    @Autowired
    private BotService botService;
    @Autowired
    private BotServiceITComponent botServiceITComponent;
    @Autowired
    private UserFacade userFacade;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final long WAITING_TIME = 10;

    @Value("${app.bot.initial-size:0}")
    private int START_FIX_BOTS_AMOUNT;

    @AfterAll
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE bot_config CASCADE");
    }

    @Test
    void generalTest() {
        Phaser phaser = new Phaser(threadCount + 1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            startBots_shouldCorrectAddedBotsInConcurrencyEnv_whenEverybodyTryingUseThisMethod(executor, phaser);
            startBots_shouldReturnFallBack_whenInDbFieldNumberOfExistingBotsAreNotEqualZero(executor, phaser);
        }
    }

    private void startBots_shouldCorrectAddedBotsInConcurrencyEnv_whenEverybodyTryingUseThisMethod(
            ExecutorService executor,
            Phaser phaser) {
        // Speed checker.........................
        long startTime = System.nanoTime();
        generalLogic(executor, phaser);
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;
        System.out.println("Execution time: " + (durationMs / 1000.0) + " seconds");

        BotConfig resultConfig = botServiceITComponent.getBotConfig();
        boolean isUserIdExists = userFacade.existsById((long) START_FIX_BOTS_AMOUNT);
        boolean isInvalidUserIdExists = userFacade.existsById((long) START_FIX_BOTS_AMOUNT + 1);

        assertThat(resultConfig).isNotNull();
        assertThat(resultConfig.getTotalBots()).isEqualTo(START_FIX_BOTS_AMOUNT);
        assertThat(isUserIdExists).isTrue();
        assertThat(isInvalidUserIdExists).isFalse();
    }

    private void startBots_shouldReturnFallBack_whenInDbFieldNumberOfExistingBotsAreNotEqualZero(
            ExecutorService executor,
            Phaser phaser) {
        generalLogic(executor, phaser);

        BotConfig resultConfig = botServiceITComponent.getBotConfig();

        assertThat(resultConfig).isNotNull();
        assertThat(resultConfig.getTotalBots()).isEqualTo(START_FIX_BOTS_AMOUNT);
    }

    private void generalLogic(ExecutorService executor, Phaser phaser) {
        int currentPhase = phaser.getPhase();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    phaser.arriveAndAwaitAdvance();
                    botService.startBots();
                } finally {
                    phaser.arrive();
                }
            });
        }
        try {
            phaser.arriveAndAwaitAdvance();
            phaser.arrive();
            phaser.awaitAdvanceInterruptibly(currentPhase + 1, WAITING_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout exception", e);
        }
    }

    @TestComponent
    static class BotServiceITComponent {
        @Autowired
        private BotConfigRepository botConfigRepository;

        @Transactional
        public BotConfig getBotConfig() {
            return botConfigRepository.findAndLock().orElse(null);
        }
    }
}
