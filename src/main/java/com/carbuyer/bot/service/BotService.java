package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.bot.dto.StopBotResponse;
import org.springframework.stereotype.Service;

@Service
public class BotService {

    public StartBotResponse startBots(StartBotRequest request) {
        return new StartBotResponse("Enabled", request.amount());
    }

    public StopBotResponse stopBots() {
        return new StopBotResponse("Disabled");
    }
}
