package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.exception.exceptions.BotConfigNotFoundException;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotServiceTest {

    @Mock
    private UserFacade userFacade;
    @Mock
    private BotConfigRepository botConfigRepository;
    @InjectMocks
    private BotService botService;

    private static final String TEST_PASSWORD = "12345678";
    private static final int TEST_AMOUNT = 10;
    private static final String ENABLED = "Enabled";

    @Test
    void startBots_shouldThrowBotConfigNotFoundException_whenBotConfigNotFound() {
        StartBotRequest request = new StartBotRequest(TEST_AMOUNT, TEST_PASSWORD);

        assertThatThrownBy(() -> botService.startBots(request))
                .isInstanceOf(BotConfigNotFoundException.class);
    }

    @Test
    void startBots_shouldReturnCorrectDto_whenBotsAddedBecauseActiveBotsNotEnough() {
        StartBotRequest request = new StartBotRequest(TEST_AMOUNT, TEST_PASSWORD);
        BotConfig config = new BotConfig();
        config.setTotalBots(TEST_AMOUNT);
        when(botConfigRepository.findAndLock()).thenReturn(Optional.of(config));
        when(userFacade.getNumberOfActiveBots()).thenReturn(0L);
        when(userFacade.getNumberOfAllInActiveBots()).thenReturn(0L);
        doNothing().when(userFacade).addBots(anyList(), anyList(), anyList());

        StartBotResponse resultDto = botService.startBots(request);

        assertThat(resultDto).isNotNull();
        assertThat(resultDto.status()).isEqualTo(ENABLED);
        assertThat(resultDto.amount()).isEqualTo(TEST_AMOUNT);

        verify(userFacade).getNumberOfActiveBots();
        verify(userFacade).getNumberOfAllInActiveBots();
        verify(userFacade).addBots(anyList(), anyList(), anyList());
    }

    @Test
    void startBots_shouldReturnCorrectDto_whenAmountLowerThanActive() {
        long activeBots = 15;
        StartBotRequest request = new StartBotRequest(TEST_AMOUNT, TEST_PASSWORD);
        BotConfig config = new BotConfig();
        config.setTotalBots((int) activeBots);
        when(botConfigRepository.findAndLock()).thenReturn(Optional.of(config));
        when(userFacade.getNumberOfActiveBots()).thenReturn(activeBots);

        StartBotResponse resultDto = botService.startBots(request);

        assertThat(resultDto).isNotNull();
        assertThat(resultDto.status()).isEqualTo(ENABLED);
        assertThat(resultDto.amount()).isEqualTo(TEST_AMOUNT);

        verify(userFacade, never()).getNumberOfAllInActiveBots();
        verify(userFacade, never()).addBots(anyList(), anyList(), anyList());
    }

    @Test
    void startBots_shouldReturnCorrectDto_whenActiveBotsAreEqualityToAmount() {
        StartBotRequest request = new StartBotRequest(TEST_AMOUNT, TEST_PASSWORD);
        BotConfig config = new BotConfig();
        config.setTotalBots(TEST_AMOUNT);
        when(botConfigRepository.findAndLock()).thenReturn(Optional.of(config));
        when(userFacade.getNumberOfActiveBots()).thenReturn((long) TEST_AMOUNT);

        StartBotResponse resultDto = botService.startBots(request);

        assertThat(resultDto).isNotNull();
        assertThat(resultDto.status()).isEqualTo(ENABLED);
        assertThat(resultDto.amount()).isEqualTo(TEST_AMOUNT);

        verify(userFacade, never()).getNumberOfAllInActiveBots();
        verify(userFacade, never()).addBots(anyList(), anyList(), anyList());
    }

    @Test
    void startBots_shouldReturnCorrectDto_whenSumOfActiveAndNotActiveMoreThanAmountInRequest() {
        int testAmount = 20;
        long testActiveBots = 5;
        long testInActiveBots = 15;
        StartBotRequest request = new StartBotRequest(TEST_AMOUNT, TEST_PASSWORD);
        BotConfig config = new BotConfig();
        config.setTotalBots(testAmount);
        when(botConfigRepository.findAndLock()).thenReturn(Optional.of(config));
        when(userFacade.getNumberOfActiveBots()).thenReturn(testActiveBots);
        when(userFacade.getNumberOfAllInActiveBots()).thenReturn(testInActiveBots);

        StartBotResponse resultDto = botService.startBots(request);

        assertThat(resultDto).isNotNull();
        assertThat(resultDto.status()).isEqualTo(ENABLED);
        assertThat(resultDto.amount()).isEqualTo(TEST_AMOUNT);

        verify(userFacade, never()).addBots(anyList(), anyList(), anyList());
    }
}
