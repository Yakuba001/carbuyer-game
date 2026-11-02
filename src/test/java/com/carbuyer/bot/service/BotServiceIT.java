package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String TEST_PASSWORD = "12345678";
    private static final long WAITING_TIME = 10;

    private static final int START_AMOUNT = 100;
    private static final int NEW_LESS_THAN_ACTIVE_AMOUNT = 60;
    private static final int NEW_MORE_THAN_ACTIVE_AMOUNT = 110;

    @AfterAll
    void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE bot_config CASCADE");
    }

    @Test
    void startBots_shouldCorrectAddedSwitchOnAndSwitchBotOffs_whenEnvironmentIsConcurrency() {
        Phaser phaser = new Phaser(threadCount + 1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            logicForFirstScenario_whenBotsNumberAddedToDbAtTheSameTimeByAnyThreads(executor, phaser);
            logicForSecondScenario_shouldDeactivateBots_whenInReqLessThenCurrActiveBots(executor, phaser);
            logicForThirdScenario_whenAmountBiggerThanActiveAndBiggerThanActivePlusInActive_shouldAddedInDbDiff(
                    executor, phaser);
            logicForFourScenario_theSameLikeThirdScenarioButInActiveAmountBiggerThanInReq_shouldSwitchOnInActive(
                    executor, phaser);
        }
    }

    private void logicForFirstScenario_whenBotsNumberAddedToDbAtTheSameTimeByAnyThreads(ExecutorService executor,
                                                                                        Phaser phaser) {
        StartBotRequest startRequest = new StartBotRequest(START_AMOUNT, TEST_PASSWORD);
        generalLogic(executor, phaser, startRequest);

        BotConfig config = botServiceITComponent.getBotConfig();
        long activeBots = userFacade.getNumberOfActiveBots();

        assertThat(config).isNotNull();
        assertThat(config.getTotalBots()).isEqualTo(START_AMOUNT);
        assertThat(activeBots).isEqualTo(START_AMOUNT);
    }

    private void logicForSecondScenario_shouldDeactivateBots_whenInReqLessThenCurrActiveBots(ExecutorService executor,
                                                                                             Phaser phaser) {
        StartBotRequest startRequest = new StartBotRequest(NEW_LESS_THAN_ACTIVE_AMOUNT, TEST_PASSWORD);
        generalLogic(executor, phaser, startRequest);
        int expectedInActiveBotsInDb = START_AMOUNT - NEW_LESS_THAN_ACTIVE_AMOUNT;

        BotConfig config = botServiceITComponent.getBotConfig();
        long activeBots = userFacade.getNumberOfActiveBots();
        long inActiveBots = userFacade.getNumberOfAllInActiveBots();

        assertThat(config).isNotNull();
        assertThat(config.getTotalBots()).isEqualTo(START_AMOUNT);
        assertThat(activeBots).isEqualTo(NEW_LESS_THAN_ACTIVE_AMOUNT);
        assertThat(inActiveBots).isEqualTo(expectedInActiveBotsInDb);
    }

    private void logicForThirdScenario_whenAmountBiggerThanActiveAndBiggerThanActivePlusInActive_shouldAddedInDbDiff(
            ExecutorService executor,
            Phaser phaser) {
        StartBotRequest request = new StartBotRequest(NEW_MORE_THAN_ACTIVE_AMOUNT, TEST_PASSWORD);
        generalLogic(executor, phaser, request);

        BotConfig config = botServiceITComponent.getBotConfig();
        long activeBots = userFacade.getNumberOfActiveBots();
        long inActiveBots = userFacade.getNumberOfAllInActiveBots();

        assertThat(config).isNotNull();
        assertThat(config.getTotalBots()).isEqualTo(NEW_MORE_THAN_ACTIVE_AMOUNT);
        assertThat(activeBots).isEqualTo(NEW_MORE_THAN_ACTIVE_AMOUNT);
        assertThat(inActiveBots).isEqualTo(0);
    }

    private void logicForFourScenario_theSameLikeThirdScenarioButInActiveAmountBiggerThanInReq_shouldSwitchOnInActive(
            ExecutorService executor,
            Phaser phaser) {
        StartBotRequest emulateDeactivationReq = new StartBotRequest(NEW_LESS_THAN_ACTIVE_AMOUNT, TEST_PASSWORD);
        botService.startBots(emulateDeactivationReq);
        StartBotRequest request = new StartBotRequest(START_AMOUNT, TEST_PASSWORD);
        generalLogic(executor, phaser, request);
        int expectedInActiveBotsInDb = NEW_MORE_THAN_ACTIVE_AMOUNT - START_AMOUNT;

        BotConfig config = botServiceITComponent.getBotConfig();
        long activeBots = userFacade.getNumberOfActiveBots();
        long inActiveBots = userFacade.getNumberOfAllInActiveBots();

        assertThat(config).isNotNull();
        assertThat(config.getTotalBots()).isEqualTo(NEW_MORE_THAN_ACTIVE_AMOUNT);
        assertThat(activeBots).isEqualTo(START_AMOUNT);
        assertThat(inActiveBots).isEqualTo(expectedInActiveBotsInDb);
    }

    private void generalLogic(ExecutorService executor, Phaser phaser, StartBotRequest request) {
        int currentPhase = phaser.getPhase();
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    phaser.arriveAndAwaitAdvance();
                    botService.startBots(request);
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
