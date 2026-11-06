package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.exception.exceptions.BotCreationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotDistributeService {

    private final BotService botService;

    public StartBotResponse botAdderManager() {
        boolean success = botService.updateStatus(ConfigStatus.IN_PROGRESS, ConfigStatus.NOT_ADDED);
        if (!success) {
            String currStatus = botService.getCurrentStatus();
            return new StartBotResponse(currStatus);
        }
        try {
            StartBotResponse response = botService.addBots();
            botService.updateStatus(ConfigStatus.ADDED, ConfigStatus.IN_PROGRESS);
            return response;
        } catch (Exception e) {
            botService.updateStatus(ConfigStatus.NOT_ADDED, ConfigStatus.IN_PROGRESS);
            throw new BotCreationException("Bot creation failed: " + e.getMessage());
        }
    }
}
