package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotDistributeServiceTest {

    @Mock
    private BotService botService;
    @Mock
    private UserFacade userFacade;
    @Mock
    private MarketFacade marketFacade;
    @InjectMocks
    private BotDistributeService distributeService;

    @Test
    void botAdderManager_shouldReturnCurrentStatusInStartBotResponse_whenThreadCantChangeCurrStatusInDb() {
        String expectedStatus = String.valueOf(ConfigStatus.IN_PROGRESS);
        when(botService.updateStatus(any(), any())).thenReturn(false);
        when(botService.getCurrentStatus()).thenReturn(expectedStatus);

        StartBotResponse result = distributeService.botAdderManager();

        assertThat(result.status()).isEqualTo(expectedStatus);

        verify(botService).updateStatus(any(), any());
        verify(botService).getCurrentStatus();
    }

    @Test
    void botAdderManager_shouldReturnStartBotResponseWithAddedStatus_whenCurrStatusNotAddedAndOtherStepsWentGood() {
        String expectedStatus = String.valueOf(ConfigStatus.ADDED);
        StartBotResponse expectedResponse = new StartBotResponse(expectedStatus);
        when(botService.updateStatus(any(), any())).thenReturn(true);
        when(botService.addBots()).thenReturn(expectedResponse);
        when(botService.updateStatus(any(), any())).thenReturn(true);

        StartBotResponse result = distributeService.botAdderManager();

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(result.status()).isEqualTo(expectedStatus);

        verify(botService, times(2)).updateStatus(any(), any());
        verify(botService, never()).getCurrentStatus();
    }

    @Test
    void botAdderManager_shouldThrowException_whenSomethingWentWrongInBotServiceWorkflow() {
        when(botService.updateStatus(any(), any())).thenReturn(true);
        when(botService.addBots()).thenThrow(new RuntimeException());
        when(botService.updateStatus(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> distributeService.botAdderManager()).isInstanceOf(BotCreationException.class);

        verify(botService, times(2)).updateStatus(any(), any());
        verify(botService, never()).getCurrentStatus();
    }

    @Test
    void startBots_shouldGoWell_whenScenarioIsHappyPath() {
        List<Long> buyerIds = List.of(1L, 2L);
        List<Long> sellerIds = List.of(4L, 5L);
        Long sellerId = sellerIds.getFirst();
        UUID listingId = UUID.randomUUID();
        int numberOfWorkerThreads = 2;
        when(botService.updateStatus(any(), any())).thenReturn(true);
        when(botService.addBots()).thenReturn(null);
        when(userFacade.getAllBuyerBotIds()).thenReturn(buyerIds);
        when(userFacade.getAllSellerBotIds()).thenReturn(sellerIds);

        when(marketFacade.getRandomListingId()).thenReturn(listingId);
        when(marketFacade.getBotSellerId(any(UUID.class))).thenReturn(sellerId);
        doNothing().when(marketFacade).buyCar(anyLong(), any(UUID.class));
        sleepThreadDurationSetter();

        distributeService.startBots(numberOfWorkerThreads);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(
                () -> verify(marketFacade, atLeastOnce()).buyCar(anyLong(), eq(listingId)));

        distributeService.stopBots();
    }

    @Test
    void startBots_shouldWaitGracefully_whenBuyersNoAvailable() {
        List<Long> sellerIds = List.of(4L, 5L);
        when(botService.updateStatus(any(), any())).thenReturn(true);
        when(botService.addBots()).thenReturn(null);
        when(userFacade.getAllBuyerBotIds()).thenReturn(List.of());
        when(userFacade.getAllSellerBotIds()).thenReturn(sellerIds);
        sleepThreadDurationSetter();

        distributeService.startBots(2);

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> verifyNoInteractions(marketFacade));

        distributeService.stopBots();
    }

    @Test
    void startBots_shouldWaitGracefully_whenListingIsNull() {
        List<Long> buyerIds = List.of(1L, 2L);
        List<Long> sellerIds = List.of(4L, 5L);
        when(botService.updateStatus(any(), any())).thenReturn(true);
        when(botService.addBots()).thenReturn(null);
        when(userFacade.getAllBuyerBotIds()).thenReturn(buyerIds);
        when(userFacade.getAllSellerBotIds()).thenReturn(sellerIds);
        when(marketFacade.getRandomListingId()).thenReturn(null);
        sleepThreadDurationSetter();

        distributeService.startBots(2);

        await().atMost(Duration.ofSeconds(3)).untilAsserted(
                () -> verify(marketFacade, never()).getBotSellerId(null));

        distributeService.stopBots();
    }

    private void sleepThreadDurationSetter() {
        Duration minThinkTimeMs = Duration.ofMillis(1);
        Duration maxThinkTimeMs = Duration.ofMillis(10);
        ReflectionTestUtils.setField(distributeService, "minThinkTime", minThinkTimeMs);
        ReflectionTestUtils.setField(distributeService, "maxThinkTime", maxThinkTimeMs);
    }
}
