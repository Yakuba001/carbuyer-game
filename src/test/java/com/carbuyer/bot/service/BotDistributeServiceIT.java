package com.carbuyer.bot.service;

import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
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
}
