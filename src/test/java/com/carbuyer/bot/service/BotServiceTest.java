package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotAddedStatus;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BotServiceTest {

    @Mock
    private UserFacade userFacade;
    @Mock
    private BotConfigRepository botConfigRepository;
    @Mock
    private CarFacade carFacade;
    @Mock
    private GarageFacade garageFacade;
    @Mock
    private MarketFacade marketFacade;
    @InjectMocks
    private BotService botService;

    @Test
    void addBots_shouldReturnStartBotResponseWithAddedStatus() {
        StartBotResponse expectedResponse = new StartBotResponse(BotAddedStatus.BOTS_ADDED.toString());
        List<Long> expectedBotIds = List.of(1L, 2L, 3L, 4L);
        List<UUID> carIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(userFacade.addBots(anyInt())).thenReturn(expectedBotIds);
        when(carFacade.addCarsByIds(anyList())).thenReturn(carIds);
        doNothing().when(garageFacade).changeEmptyGarageSlotStatusAndSetInItCarId(anyList(), anyList());
        doNothing().when(marketFacade).createListings(anyList(), anyList());

        StartBotResponse result = botService.addBots();

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void updateStatus_shouldReturnTrue_whenStatusChanged() {
        ConfigStatus curr = ConfigStatus.NOT_ADDED;
        ConfigStatus newStatus = ConfigStatus.ADDED;
        when(botConfigRepository.updateStatusIfExpected(any(), any())).thenReturn(1);

        boolean result = botService.updateStatus(curr, newStatus);

        assertThat(result).isTrue();
    }

    @Test
    void updateStatus_shouldReturnFalse_whenStatusDidNotChanged() {
        ConfigStatus curr = ConfigStatus.ADDED;
        ConfigStatus newStatus = ConfigStatus.ADDED;
        when(botConfigRepository.updateStatusIfExpected(any(), any())).thenReturn(0);

        boolean result = botService.updateStatus(curr, newStatus);

        assertThat(result).isFalse();
    }

    @Test
    void getCurrentStatus_shouldReturnBotsAddedInProgressString_whenConfigStatusInProgress() {
        String currStatus = ConfigStatus.IN_PROGRESS.toString();
        String expectedStatus = BotAddedStatus.BOTS_ADDED_IN_PROGRESS.toString();
        when(botConfigRepository.getConfigStatus()).thenReturn(currStatus);

        String result = botService.getCurrentStatus();

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    void getCurrentStatus_shouldReturnBotsAdded_whenConfigStatusNotInProgress() {
        String currStatus = ConfigStatus.NOT_ADDED.toString();
        String expectedStatus = BotAddedStatus.BOTS_ADDED.toString();
        when(botConfigRepository.getConfigStatus()).thenReturn(currStatus);

        String result = botService.getCurrentStatus();

        assertThat(result).isEqualTo(expectedStatus);
    }
}
