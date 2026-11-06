package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.exception.exceptions.BotCreationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotDistributeServiceTest {

    @Mock
    private BotService botService;
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
}
