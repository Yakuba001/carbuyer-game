package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.repository.BotConfigRepository;
import com.carbuyer.exception.exceptions.BotConfigNotFoundException;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractList;
import java.util.List;
import java.util.UUID;
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

    @Transactional
    public StartBotResponse startBots(StartBotRequest request) {
        BotConfig config = botConfigRepository.findAndLock()
                .orElseThrow(() -> new BotConfigNotFoundException("Bot config not found"));
        int amount = request.amount();
        long currentNumberActiveBots = userFacade.getNumberOfActiveBots();
        long currentNumberInActiveBots = 0;
        if (amount < currentNumberActiveBots) {
            deactivateBots(currentNumberActiveBots - amount);
            return new StartBotResponse(BOTS_ENABLED, amount);
        } else if (amount == currentNumberActiveBots) {
            return new StartBotResponse(BOTS_ENABLED, amount);
        } else if (amount > currentNumberActiveBots) {
            currentNumberInActiveBots = userFacade.getNumberOfAllInActiveBots();
            int diff = amount - (int) (currentNumberActiveBots + currentNumberInActiveBots);
            if (diff > 0) {
                if (currentNumberInActiveBots > 0) {
                    activateBots(currentNumberInActiveBots);
                }
                userFacade.addBots(
                        uniqueNamesGenerator(diff),
                        uniqueEmailsGenerator(diff),
                        uniqueAndEncodedPasswordGenerator(diff, request.botPassword()));
            } else {
                if (currentNumberInActiveBots > diff) {
                    activateBots(currentNumberInActiveBots - diff);
                }
            }
        }
        int totalBots = amount > currentNumberActiveBots + currentNumberInActiveBots ? amount :
                (int) (currentNumberActiveBots + currentNumberInActiveBots);
        config.setTotalBots(totalBots);
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
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void activateBots(long amount) {
        userFacade.activateBots(amount);
    }

    private void deactivateBots(long amount) {
        userFacade.deactivateBots(amount);
    }
}
