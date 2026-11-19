package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StopBotResponse;
import com.carbuyer.bot.entity.BotStatus;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.exception.exceptions.InsufficientFundsException;
import com.carbuyer.exception.exceptions.InvalidBotAmountException;
import com.carbuyer.exception.exceptions.StartBotsException;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotServiceTest {

    @Mock
    private BotCore botCore;
    @Spy
    @InjectMocks
    private BotService botService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(botService, "minThinkTime", Duration.ofSeconds(1));
        ReflectionTestUtils.setField(botService, "maxThinkTime", Duration.ofSeconds(1));
    }

    @Test
    void startBots_shouldReturnCorrectResponse_whenBotsAlreadyStarted() {
        ReflectionTestUtils.setField(botService, "isBotsRunning", true);
        StartBotResponse expectedResponse = new StartBotResponse(BotStatus.ALREADY_STARTED.toString(), 0);

        StartBotResponse resultResponse = botService.startBots(228);

        assertThat(resultResponse).isEqualTo(expectedResponse);
    }

    @Test
    void startBots_shouldThrowInvalidBotAmountException_whenNumberOfWorkerThreadsIsNegative() {
        assertThrows(InvalidBotAmountException.class, () -> botService.startBots(-1));
    }

    @Test
    void startBots_shouldStopBotsAndThrowStartBotsException_whenUnexpectedExceptionOccurs() {
        int count = 10;
        doThrow(InsufficientFundsException.class).when(botCore).syncBotsCountInDb();

        assertThatThrownBy(() -> botService.startBots(count))
                .isInstanceOf(StartBotsException.class)
                .hasCauseInstanceOf(InsufficientFundsException.class);

        verify(botService).stopBots();
    }

    @Test
    void startBots_shouldStopBotsAndThrowStartBotsException_whenTotalBotsInSysIsZero() {
        int count = 10;
        when(botCore.getIds(anyBoolean())).thenReturn(List.of());

        assertThrows(StartBotsException.class, () -> botService.startBots(count));

        verify(botService).stopBots();
    }

    @Test
    void startBots_shouldSuccessfullyAddedBotsInSysAndStartCorrectWorkersAmount_whenAddedToManyWorkersInRequest() {
        int count = 1000;
        when(botCore.getIds(true)).thenReturn(List.of(1L, 2L));
        when(botCore.getIds(false)).thenReturn(List.of(3L, 4L));
        StartBotResponse expectedResponse =
                new StartBotResponse(BotStatus.SUCCESSFULLY_STARTED.toString(), 2);

        StartBotResponse resultResponse = botService.startBots(count);

        assertThat(resultResponse).isEqualTo(expectedResponse);
    }

    // TODO: in startBots method check this branches too: runSimulationLoop / tryRebalance / getIdsCollectionToRebalance

    @Test
    void startBots_shouldSuccessfullyAddedBotsInSysAndStartWorkers_whenEverythingWentWell() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of(1L, 2L));
        when(botCore.getIds(false)).thenReturn(List.of(3L, 4L));
        StartBotResponse expectedResponse = new StartBotResponse(BotStatus.SUCCESSFULLY_STARTED.toString(), count);

        StartBotResponse resultResponse = botService.startBots(count);

        assertThat(resultResponse).isEqualTo(expectedResponse);
    }

    @Test
    void stopBots_shouldResetStateAndReturnStoppedStatus_whenBotsWereRunning() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of(1L, 2L));
        when(botCore.getIds(false)).thenReturn(List.of(3L, 4L));
        botService.startBots(count);
        StopBotResponse expectedResponse = new StopBotResponse(BotStatus.STOPPED.toString());
        StartBotResponse expectedStartResponse = new StartBotResponse(BotStatus.SUCCESSFULLY_STARTED.toString(), count);

        StopBotResponse response = botService.stopBots();

        assertThat(response).isEqualTo(expectedResponse);

        StartBotResponse restartResponse = botService.startBots(count);
        assertThat(restartResponse).isEqualTo(expectedStartResponse);
    }

    @Test
    void stopBots_shouldNotThrowException_whenBotsWereNotStarted() {
        StopBotResponse expectedResponse = new StopBotResponse(BotStatus.STOPPED.toString());

        StopBotResponse result = botService.stopBots();

        assertThat(result).isEqualTo(expectedResponse);
    }
}
