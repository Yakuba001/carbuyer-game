package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StartBotRequest;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public StartBotResponse startBots(StartBotRequest request) {
        int amount = request.amount();
        long currentAmount = userFacade.countingExistingBots(BOTS_DOMAIN);
        if (amount < currentAmount) {
            return new StartBotResponse(BOTS_ENABLED, (int) currentAmount);
        } else if (amount == currentAmount) {
            return new StartBotResponse(BOTS_ENABLED, amount);
        } else if (amount > currentAmount) {
            int diff = (int) (amount - currentAmount);

            // TODO: create a guaranties if bots exists with some names/emails,
            //  new execution won`t create bots with same email/name, to avoid conflict, maybe try get map by bot prefix name,
            //  sorted and check the highest index,
            //  after that just start from that index added some new bots to avoid conflicts or something like this.....

            userFacade.addBots(
                    uniqueNamesGenerator(diff),
                    uniqueEmailsGenerator(diff),
                    uniqueAndEncodedPasswordGenerator(diff, request.botPassword()));
        }
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
                return "CarBuyerBot" + index;
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
                return "email" + index + BOTS_DOMAIN;
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
                return passwordEncoder.encode(password + index);
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
}
