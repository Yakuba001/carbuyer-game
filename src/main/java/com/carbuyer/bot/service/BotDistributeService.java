package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public StartBotResponse botAdderManager() {
        boolean success = botService.updateStatus(ConfigStatus.IN_PROGRESS, ConfigStatus.NOT_ADDED);
        if (!success) {
            String currStatus = botService.getCurrentStatus();
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
    public synchronized void startBots() {
        if (isBotsRunning) return;
        botAdderManager();
        isBotsRunning = true;
        threadPool = Executors.newVirtualThreadPerTaskExecutor();

        // TODO: just stubbing
        int parallelWorkers = 10;

        for (int i = 0; i < parallelWorkers; i++) {
            threadPool.execute(this::runSimulationLoop);
        }
    }
    // TODO: create endpoints too
    public synchronized void stopBots() {
        isBotsRunning = false;
        close();
    }
    // TODO: check how its working in concurrency environment and all in all work or not................
    private void runSimulationLoop() {
        while (isBotsRunning && !Thread.currentThread().isInterrupted()) {
            try {
                int thinkTime = ThreadLocalRandom.current().nextInt(1, 5);
                TimeUnit.SECONDS.sleep(thinkTime);
                Long buyerId = this.buyerIds.pollFirst();
                if (buyerId == null) {
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }
                UUID listingId = marketFacade.getRandomListingId();
                if (listingId == null) {
                    this.buyerIds.addLast(buyerId);
                    TimeUnit.SECONDS.sleep(1);
                    continue;
                }
                try {
                    Long sellerId = marketFacade.getBotSellerId(listingId);
                    marketFacade.buyCar(buyerId, listingId);
                    this.sellerIds.addLast(buyerId);
                    if (sellerId != null) {
                        this.sellerIds.remove(sellerId);
                        this.buyerIds.addLast(sellerId);
                        log.info("Bot {} bought a car from {} (Listing: {})", buyerId, sellerId, listingId);
                    }
                } catch (Exception e) {
                    this.buyerIds.addLast(buyerId);
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
}
