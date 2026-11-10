package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.bot.entity.BotAddedStatus;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BotService {

    @Value("${app.bot.initial-size:0}")
    private int START_FIX_BOTS_AMOUNT;

    private final UserFacade userFacade;
    private final BotConfigRepository botConfigRepository;
    private final CarFacade carFacade;
    private final GarageFacade garageFacade;
    private final MarketFacade marketFacade;

    @Transactional
    public StartBotResponse addBots() {
        addBotsCarsAndListingsInSystem();
        return new StartBotResponse(BotAddedStatus.BOTS_ADDED.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateStatus(ConfigStatus newStatus, ConfigStatus expectedStatus) {
        int updatedRows = botConfigRepository.updateStatusIfExpected(newStatus, expectedStatus);
        return updatedRows > 0;
    }

    @Transactional(readOnly = true)
    public String getCurrentStatus() {
        ConfigStatus status = ConfigStatus.valueOf(botConfigRepository.getConfigStatus());
        String result;
        if (status == ConfigStatus.IN_PROGRESS) {
            result = BotAddedStatus.BOTS_ADDED_IN_PROGRESS.toString();
        } else result = BotAddedStatus.BOTS_ADDED.toString();
        return result;
    }

    private void addBotsCarsAndListingsInSystem() {
        List<Long> allAddedBotIds = userFacade.addBots(START_FIX_BOTS_AMOUNT);
        List<Long> sellers = new ArrayList<>(allAddedBotIds.subList(0, allAddedBotIds.size() / 2));
        List<UUID> carIds = carFacade.addCarsByIds(sellers);
        garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(sellers, carIds);
        marketFacade.createListings(sellers, carIds);
    }
}
