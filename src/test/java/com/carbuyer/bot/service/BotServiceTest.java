package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.exception.exceptions.BotConfigNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotServiceTest {

    @Mock
    private BotConfigRepository botConfigRepository;
    @InjectMocks
    private BotService botService;

    @Test
    void startBots_shouldThrowBotConfigNotFoundException_whenBotConfigNotFound() {
        when(botConfigRepository.findAndLock()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> botService.startBots()).isInstanceOf(BotConfigNotFoundException.class);
    }

    @Test
    void startBots_shouldReturnCorrectDto_whenEverythingWentCorrect() {
        BotConfig botConfig = new BotConfig();
        botConfig.setTotalBots(100);
        String expectedStatus = "Bots enabled";
        when(botConfigRepository.findAndLock()).thenReturn(Optional.of(botConfig));

        StartBotResponse startBotResponse = botService.startBots();

        assertThat(startBotResponse).isNotNull();
        assertThat(startBotResponse.status()).isEqualTo(expectedStatus);
    }
}
