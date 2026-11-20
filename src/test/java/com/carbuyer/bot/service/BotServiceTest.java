package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StopBotResponse;
import com.carbuyer.bot.entity.BotStatus;
import com.carbuyer.exception.exceptions.InsufficientFundsException;
import com.carbuyer.exception.exceptions.InvalidBotAmountException;
import com.carbuyer.exception.exceptions.StartBotsException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ReflectionTestUtils.setField(botService, "minThinkTime", Duration.ofMillis(1));
        ReflectionTestUtils.setField(botService, "maxThinkTime", Duration.ofMillis(1));
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

    @Test
    void tryRebalance_shouldEngageWithBotCoreToAddCarsAndListings_whenSellerRatioLowerThanNeed() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of());
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L));

        botService.startBots(count);

        verify(botCore, timeout(1000).atLeast(2)).getIds(anyBoolean());
        verify(botCore, timeout(50)).carAndListingCreator(anyList());
        verify(botCore, never()).carAndListingDeleter(anyList());
    }

    @Test
    void tryRebalance_shouldDoNothing_whenSellerIdsToCreateIsEmpty() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of()).thenReturn(List.of());
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L)).thenReturn(List.of(1L));

        botService.startBots(count);

        verify(botCore, timeout(1000).atLeast(2)).getIds(anyBoolean());
        verify(botCore, after(50).never()).carAndListingCreator(anyList());
        verify(botCore, never()).carAndListingDeleter(anyList());
    }

    @Test
    void tryRebalance_shouldDoNothing_whenBuyerIdsToCreateIsEmpty() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of()).thenReturn(List.of(1L));
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L)).thenReturn(List.of());

        botService.startBots(count);

        verify(botCore, never()).carAndListingCreator(anyList());
        verify(botCore, after(1000).never()).carAndListingDeleter(anyList());
    }

    @Test
    void tryRebalance_shouldEngageWithBotCoreToDeleteCarsAndListings_whenBuyerRatioLowerThanNeed() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of()).thenReturn(List.of(1L, 2L));
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L)).thenReturn(List.of());

        botService.startBots(count);

        verify(botCore, never()).carAndListingCreator(anyList());
        verify(botCore, timeout(1000)).carAndListingDeleter(anyList());
    }

    @Test
    void tryRebalance_shouldHandleExceptionAndReturnIdsInBuyerSeq_whenCarAndListingCreatorThrowException() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of());
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L));
        doThrow(RuntimeException.class)
                .doNothing()
                .when(botCore).carAndListingCreator(anyList());

        botService.startBots(count);

        verify(botCore, timeout(1000).atLeast(2)).carAndListingCreator(anyList());
    }

    @Test
    void tryRebalance_shouldHandleExceptionAndReturnIdsInSellerSeq_whenCarAndListingDeleterThrowException() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of()).thenReturn(List.of(1L, 2L));
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L)).thenReturn(List.of());
        doThrow(RuntimeException.class)
                .doNothing()
                .when(botCore).carAndListingDeleter(anyList());

        botService.startBots(count);

        verify(botCore, timeout(1000).atLeast(2)).carAndListingDeleter(anyList());
    }

    @Test
    void tryRebalance_shouldDoNothing_whenTotalBotsIsZero() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of()).thenReturn(List.of());
        when(botCore.getIds(false)).thenReturn(List.of(1L, 2L)).thenReturn(List.of());

        botService.startBots(count);

        verify(botCore, never()).carAndListingCreator(anyList());
        verify(botCore, never()).carAndListingDeleter(anyList());
    }

    @Test
    void runSimulationLoop_shouldHoldTheSameBuyerIdInExecuteBotCarPurchase_whenAfterFirstTryReturnsNullDto() {
        int count = 1;
        long buyerId = 1L;
        when(botCore.getIds(true)).thenReturn(List.of(2L));
        when(botCore.getIds(false)).thenReturn(List.of(buyerId));
        when(botCore.executeBotCarPurchase(buyerId))
                .thenReturn(null)
                .thenReturn(new PurchaseResult(null, null, null));

        botService.startBots(count);

        verify(botCore, timeout(1000).atLeast(2)).executeBotCarPurchase(buyerId);
    }

    @Test
    void runSimulationLoop_shouldExecuteBotListingCreator_whenEverythingWentPerfect() {
        int count = 1;
        long buyerId = 1L;
        UUID carId = UUID.randomUUID();
        when(botCore.getIds(true)).thenReturn(List.of(2L));
        when(botCore.getIds(false)).thenReturn(List.of(buyerId));
        when(botCore.executeBotCarPurchase(buyerId))
                .thenReturn(new PurchaseResult(null, carId, null));

        botService.startBots(count);

        verify(botCore, timeout(1000)).listingCreator(buyerId, carId);
    }

    @Test
    void runSimulationLoop_shouldUseTheSameBuyerId_whenSomeExceptionThrowsDuringTheCycle() {
        int count = 1;
        long buyerId = 1L;
        UUID carId = UUID.randomUUID();
        when(botCore.getIds(true)).thenReturn(List.of(2L));
        when(botCore.getIds(false)).thenReturn(List.of(buyerId));
        when(botCore.executeBotCarPurchase(buyerId))
                .thenReturn(new PurchaseResult(null, carId, null));
        doThrow(RuntimeException.class).when(botCore).listingCreator(buyerId, carId);

        botService.startBots(count);

        verify(botCore, timeout(1000).atLeast(2)).executeBotCarPurchase(buyerId);
    }

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

    @Test
    void stopBots_shouldBeIdempotent_whenBotsStoppedTwice() {
        int count = 1;
        when(botCore.getIds(true)).thenReturn(List.of(1L, 2L));
        when(botCore.getIds(false)).thenReturn(List.of(3L, 4L));
        botService.startBots(count);

        assertDoesNotThrow(() -> {
            botService.stopBots();
            botService.stopBots();
        });
    }
}
