package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.BotConfigNotFoundException;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BotService implements AutoCloseable {

    private static final String BOTS_ENABLED = "Enabled";
    private static final String BOTS_DOMAIN = "@carbuyer.com";
    private static final ExecutorService THREAD_POOL = Executors.newVirtualThreadPerTaskExecutor();

    private final PasswordEncoder passwordEncoder;
    private final UserFacade userFacade;
    private final BotConfigRepository botConfigRepository;
    private final CarFacade carFacade;
    private final GarageFacade garageFacade;

    @Transactional
    public StartBotResponse startBots(StartBotRequest request) {
        BotConfig config = botConfigRepository.findAndLock()
                .orElseThrow(() -> new BotConfigNotFoundException("Bot config not found"));
        int amount = request.amount();
        int currentNumberActiveBots = userFacade.getNumberOfActiveBots();
        int currentNumberInActiveBots;
        if (amount < currentNumberActiveBots) {
            currentNumberActiveBots = currentNumberActiveBots - amount;
            userFacade.deactivateBots(currentNumberActiveBots);
            // TODO: when bots becomes inActive - should change garageSlotStatus to empty and set carId null and deleteCarFromDb
            carOwnersRatioController(currentNumberActiveBots);
            return new StartBotResponse(BOTS_ENABLED, amount);
        } else if (amount == currentNumberActiveBots) {
            return new StartBotResponse(BOTS_ENABLED, amount);
        } else {
            currentNumberInActiveBots = userFacade.getNumberOfInActiveBots();
            int diff = amount - (currentNumberActiveBots + currentNumberInActiveBots);
            if (diff > 0) {
                if (currentNumberInActiveBots > 0) {
                    userFacade.activateBots(currentNumberInActiveBots);
                }
                userFacade.addBots(
                        uniqueNamesGenerator(diff),
                        uniqueEmailsGenerator(diff),
                        uniqueAndEncodedPasswordGenerator(diff, request.botPassword()));
            } else {
                if (currentNumberInActiveBots > diff) {
                    userFacade.activateBots(currentNumberInActiveBots - diff);
                }
            }
        }
        int totalBots = Math.max(amount, currentNumberActiveBots + currentNumberInActiveBots);
        config.setTotalBots(totalBots);
        currentNumberActiveBots = userFacade.getNumberOfActiveBots();
        carOwnersRatioController(currentNumberActiveBots);
        return new StartBotResponse(BOTS_ENABLED, amount);
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

    private void carOwnersRatioController(int allActiveBots) {
        List<Long> idsList = userFacade.getActiveBotIdsWithLimit(allActiveBots);
        int amountActiveBotsWithEmptyGarageSlot = idsList.size();
        int halfSize = amountActiveBotsWithEmptyGarageSlot / 2;
        List<Long> halfList;
        int diff = amountActiveBotsWithEmptyGarageSlot - allActiveBots;
        if (diff == 0) {
            halfList = idsList.subList(0, halfSize);
        } else if (diff > 0) {
            // TODO: solve it its just stubbing
            halfList = idsList.subList(0, halfSize);
        } else {
            // TODO: solve it its just stubbing
            halfList = idsList.subList(0, halfSize);
        }
        carFacade.addCarsByIds(halfList);
    }

    private List<String> uniqueNamesGenerator(int amount) {
        return new AbstractList<>() {
            @Override
            public String get(int index) {
                checkSizeBoundaries(index, amount);
                return "CarBuyerBot" + saltGenerator();
            }

            @Override
            public int size() {
                return amount;
            }
        };
    }

    private List<String> uniqueEmailsGenerator(int amount) {
        return new AbstractList<>() {
            @Override
            public String get(int index) {
                checkSizeBoundaries(index, amount);
                return "email" + saltGenerator() + BOTS_DOMAIN;
            }

            @Override
            public int size() {
                return amount;
            }
        };
    }

    private List<String> uniqueAndEncodedPasswordGenerator(int amount, String password) {
        return new AbstractList<>() {
            @Override
            public String get(int index) {
                checkSizeBoundaries(index, amount);
                return passwordEncoder.encode(password + saltGenerator());
            }

            @Override
            public int size() {
                return amount;
            }
        };
    }

    private void checkSizeBoundaries(int index, int amount) {
        if (index < 0 || index >= amount) {
            throw new IndexOutOfBoundsException();
        }
    }

    private String saltGenerator() {
        return RandomStringUtils.randomAlphanumeric(8);
    }
}
