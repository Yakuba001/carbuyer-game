package com.carbuyer.bot.service;

import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class BotDistributeServiceIT extends BaseIntegrationTest {

    @Autowired
    private BotDistributeService distributeService;
    @Autowired
    private BotService botService;
    @Autowired
    private UserFacade userFacade;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void tearDown() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
        jdbcTemplate.update("UPDATE bot_config SET status = 'NOT_ADDED' WHERE id = 1");
    }

    @Test
    void startBots_shouldGoWellAndAvoidDeadLocks_whenScenarioIsHappyPath() {
        int startBotAmount = 11;
        int expectedHalf = startBotAmount / 2;
        ReflectionTestUtils.setField(botService, "START_FIX_BOTS_AMOUNT", startBotAmount);
        distributeService.startBots(20);

        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        distributeService.stopBots();

        List<Long> resultBuyerList = userFacade.getAllBuyerBotIds();
        List<Long> resultSellerList = userFacade.getAllSellerBotIds();

        assertThat(resultSellerList.size() + resultBuyerList.size()).isEqualTo(startBotAmount);
        assertThat(resultSellerList.size()).isCloseTo(expectedHalf, within(1));
        assertThat(resultBuyerList.size()).isCloseTo(expectedHalf, within(1));
    }

    @Test
    void startBots_shouldWorkCorrectly_whenExecutedConcurrently() {
        int startBotAmount = 11;
        int expectedHalf = startBotAmount / 2;
        ReflectionTestUtils.setField(botService, "START_FIX_BOTS_AMOUNT", startBotAmount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> distributeService.startBots(20));
            }
        }
        distributeService.stopBots();

        List<Long> resultBuyerList = userFacade.getAllBuyerBotIds();
        List<Long> resultSellerList = userFacade.getAllSellerBotIds();

        assertThat(resultSellerList.size() + resultBuyerList.size()).isEqualTo(startBotAmount);
        assertThat(resultSellerList.size()).isCloseTo(expectedHalf, within(1));
        assertThat(resultBuyerList.size()).isCloseTo(expectedHalf, within(1));
    }

    @Test
    void startBots_shouldDoNothing_whenNumberOfWorkerThreadsIsZero() {
        distributeService.startBots(0);

        List<Long> resultBuyerList = userFacade.getAllBuyerBotIds();
        List<Long> resultSellerList = userFacade.getAllSellerBotIds();

        assertThat(resultBuyerList.size()).isEqualTo(0);
        assertThat(resultSellerList.size()).isEqualTo(0);
    }
}
