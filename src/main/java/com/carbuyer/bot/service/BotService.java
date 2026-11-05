package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.BotConfigNotFoundException;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BotService implements AutoCloseable {

    private static final String BOTS_ENABLED = "Bots enabled";
    @Value("${app.bot.initial-size:0}")
    private int START_FIX_BOTS_AMOUNT;

    private static final ExecutorService THREAD_POOL = Executors.newVirtualThreadPerTaskExecutor();
    private List<Long> botSellerIdsList;
    private List<Long> botBuyerIdsList;

    private final UserFacade userFacade;
    private final BotConfigRepository botConfigRepository;
    private final CarFacade carFacade;
    private final GarageFacade garageFacade;
    private final MarketFacade marketFacade;

    // TODO: solve transaction starvation problem and try to find better speed realisation
    @Transactional
    public StartBotResponse startBots() {
        BotConfig botConfig = botConfigRepository.findAndLock()
                .orElseThrow(() -> new BotConfigNotFoundException("Bot config not found"));
        if (botConfig.getTotalBots() == 0) {
            addBotsCarsAndListingsInSystem();
            botConfig.setTotalBots(START_FIX_BOTS_AMOUNT);
        }
        return new StartBotResponse(BOTS_ENABLED);
    }

    @Override
    public void close() {
        THREAD_POOL.shutdown();
        try {
            if (!THREAD_POOL.awaitTermination(10, TimeUnit.SECONDS)) {
                THREAD_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            THREAD_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void addBotsCarsAndListingsInSystem() {
        List<Long> allAddedBotIds = userFacade.addBots(START_FIX_BOTS_AMOUNT);
        List<Long> sellers = new ArrayList<>(allAddedBotIds.subList(0, allAddedBotIds.size() / 2));
        List<Long> buyers = new ArrayList<>(allAddedBotIds.subList(allAddedBotIds.size() / 2, allAddedBotIds.size()));
        List<UUID> carIds = carFacade.addCarsByIds(sellers);
        garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(sellers, carIds);
        marketFacade.createListings(sellers, carIds);

        this.botSellerIdsList = sellers;
        this.botBuyerIdsList = buyers;
    }
}
