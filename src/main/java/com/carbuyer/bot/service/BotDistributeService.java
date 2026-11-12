package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotDistributeService implements AutoCloseable {

    private final BotService botService;
    private final UserFacade userFacade;
    private final MarketFacade marketFacade;

    private ExecutorService threadPool;
    private volatile boolean isBotsRunning = false;
    private final Deque<Long> buyerIds = new ConcurrentLinkedDeque<>();
    private final Deque<Long> sellerIds = new ConcurrentLinkedDeque<>();

    @Value("${app.bot.think-time.min:1000}")
    private Duration minThinkTime;
    @Value("${app.bot.think-time.max:5000}")
    private Duration maxThinkTime;

    public StartBotResponse botAdderManager() {
        boolean success = botService.updateStatus(ConfigStatus.IN_PROGRESS, ConfigStatus.NOT_ADDED);
        if (!success) {
            String currStatus = botService.getCurrentStatus();
            log.info("Enter here");
            collectionSync();
            return new StartBotResponse(currStatus);
        }
        try {
            StartBotResponse response = botService.addBots();
            botService.updateStatus(ConfigStatus.ADDED, ConfigStatus.IN_PROGRESS);
            collectionSync();
            return response;
        } catch (Exception e) {
            botService.updateStatus(ConfigStatus.NOT_ADDED, ConfigStatus.IN_PROGRESS);
            throw new BotCreationException("Bot creation failed: " + e.getMessage());
        }
    }

    // TODO: will add endpoints in controller
    public synchronized void startBots(int numberOfWorkerThreads) {
        if (isBotsRunning || numberOfWorkerThreads <= 0) return;
        botAdderManager();
        isBotsRunning = true;
        threadPool = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < numberOfWorkerThreads; i++) {
            threadPool.execute(this::runSimulationLoop);
        }
    }

    // TODO: create endpoints too
    public synchronized void stopBots() {
        isBotsRunning = false;
        close();
    }

    private void runSimulationLoop() {
        while (isBotsRunning && !Thread.currentThread().isInterrupted()) {
            try {
                long minMs = minThinkTime.toMillis();
                long maxMs = maxThinkTime.toMillis();
                long thinkTimeMs = (minMs >= maxMs) ? minMs : ThreadLocalRandom.current().nextLong(minMs, maxMs);
                TimeUnit.MILLISECONDS.sleep(thinkTimeMs);
                Long buyerId = this.buyerIds.pollFirst();
                if (buyerId == null) {
                    TimeUnit.MILLISECONDS.sleep(thinkTimeMs);
                    log.info("BuyerId is empty");
                    continue;
                }
                UUID listingId = marketFacade.getRandomListingId();
                if (listingId == null) {
                    this.buyerIds.addLast(buyerId);
                    TimeUnit.MILLISECONDS.sleep(thinkTimeMs);
                    log.info("ListingId is empty");
                    continue;
                }
                UUID listingCarId = marketFacade.getListingCarId(listingId);
                if (listingCarId == null) {
                    this.buyerIds.addLast(buyerId);
                    TimeUnit.MILLISECONDS.sleep(thinkTimeMs);
                    log.info("ListingCarId is empty");
                    continue;
                }
                try {
                    Long sellerId = marketFacade.getBotSellerId(listingId);
                    marketFacade.buyCar(buyerId, listingId);
                    marketFacade.createBotListing(buyerId, listingCarId, priceGenerator());
                    this.sellerIds.addLast(buyerId);
                    if (sellerId != null) {
                        this.sellerIds.remove(sellerId);
                        this.buyerIds.addLast(sellerId);
                        log.info("Bot {} bought a car from Bot {} (Listing: {})", buyerId, sellerId, listingId);
                    }
                    log.info("New listing is created");
                } catch (Exception e) {
                    this.buyerIds.addLast(buyerId);
                    log.info(e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void collectionSync() {
        this.buyerIds.clear();
        this.sellerIds.clear();
        List<Long> buyerIds = userFacade.getAllBuyerBotIds();
        List<Long> sellerIds = userFacade.getAllSellerBotIds();
        this.buyerIds.addAll(buyerIds);
        this.sellerIds.addAll(sellerIds);
    }
    // TODO: thinking about economic...
    private BigDecimal priceGenerator() {
        double lowerBound = 10.00;
        double upperBound = 100.00;
        return new BigDecimal(ThreadLocalRandom.current().nextDouble(lowerBound, upperBound));
    }
}
