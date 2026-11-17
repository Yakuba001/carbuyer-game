package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StopBotResponse;
import com.carbuyer.bot.entity.BotStatus;
import com.carbuyer.exception.exceptions.StartBotsException;
import com.carbuyer.exception.exceptions.InvalidBotAmountException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotService implements AutoCloseable {

    private final BotCore botCore;

    private ExecutorService threadPool;
    private int currentWorkersAmount;
    private volatile boolean isBotsRunning = false;

    private final Deque<Long> buyerIds = new ConcurrentLinkedDeque<>();
    private final Deque<Long> sellerIds = new ConcurrentLinkedDeque<>();
    private final Set<Long> inFlightBots = ConcurrentHashMap.newKeySet();

    @Value("${app.bot.think-time.min:1000}")
    private Duration minThinkTime;
    @Value("${app.bot.think-time.max:5000}")
    private Duration maxThinkTime;
    private static final double MIN_RATIO_THRESHOLD = 0.30;
    private static final double TARGET_RATIO = 0.50;
    private static final int SYNC_EVERY_N_ITERATIONS = 10;

    private final ReentrantLock resizeRatioLock = new ReentrantLock();
    private final AtomicInteger iterationCounter = new AtomicInteger(0);

    public synchronized StartBotResponse startBots(int numberOfWorkerThreads) {
        if (isBotsRunning) {
            return new StartBotResponse(BotStatus.ALREADY_STARTED.toString(), currentWorkersAmount);
        }
        if (numberOfWorkerThreads <= 0) {
            throw new InvalidBotAmountException("Number of worker threads must be greater than 0");
        }
        try {
            if (!botCore.isBotsAlreadyExistsInDb()) {
                botCore.addBots();
            }
            threadStarter(numberOfWorkerThreads);
            return new StartBotResponse(BotStatus.SUCCESSFULLY_STARTED.toString(), currentWorkersAmount);
        } catch (Exception e) {
            stopBots();
            throw new StartBotsException("An unexpected error occurred while starting bots", e);
        }
    }

    public synchronized StopBotResponse stopBots() {
        close();
        return new StopBotResponse(BotStatus.STOPPED.toString());
    }

    // TODO: should try to add netherlands auction to avoid financial bot problems.
    private void runSimulationLoop() {
        while (isBotsRunning && !Thread.currentThread().isInterrupted()) {
            try {
                long minMs = minThinkTime.toMillis();
                long maxMs = maxThinkTime.toMillis();
                long thinkTimeMs = (minMs >= maxMs) ? minMs : ThreadLocalRandom.current().nextLong(minMs, maxMs);
                TimeUnit.MILLISECONDS.sleep(thinkTimeMs);
                Long buyerId = buyerIds.pollFirst();
                if (buyerId == null) {
                    tryRebalance(true);
                    continue;
                }
                inFlightBots.add(buyerId);
                try {
                    PurchaseResult purchaseResult = botCore.executeBotCarPurchase(buyerId);
                    if (purchaseResult == null) {
                        inFlightBots.remove(buyerId);
                        buyerIds.addLast(buyerId);
                        tryRebalance(true);
                        continue;
                    }
                    UUID listingId = purchaseResult.listingId();
                    UUID listingCarId = purchaseResult.carId();
                    Long sellerId = purchaseResult.sellerId();
                    botCore.listingCreator(buyerId, listingCarId);
                    this.sellerIds.addLast(buyerId);
                    if (sellerId != null) {
                        this.sellerIds.remove(sellerId);
                        this.buyerIds.addLast(sellerId);
                        log.info("Bot {} bought a car from Bot {} (Listing: {})", buyerId, sellerId, listingId);
                        log.info("New listing is created");
                    }
                    boolean forceSync = iterationCounter.incrementAndGet() % SYNC_EVERY_N_ITERATIONS == 0;
                    tryRebalance(forceSync);
                } catch (Exception e) {
                    this.buyerIds.addLast(buyerId);
                    log.warn("Simulation step failed for bot {}: {}", buyerId, e.getMessage());
                } finally {
                    inFlightBots.remove(buyerId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void tryRebalance(boolean forceSync) {
        if (!resizeRatioLock.tryLock()) {
            return;
        }
        try {
            if (forceSync) {
                collectionSync();
            }
            int buyerCount = buyerIds.size();
            int sellerCount = sellerIds.size();
            int totalBots = buyerCount + sellerCount + inFlightBots.size();
            if (totalBots == 0) {
                return;
            }
            double buyerRatio = (double) buyerCount / totalBots;
            double sellerRatio = (double) sellerCount / totalBots;
            log.info("Buyer count: {}, seller count: {}, inFlight: {}", buyerCount, sellerCount, inFlightBots.size());
            if (sellerRatio < MIN_RATIO_THRESHOLD) {
                List<Long> buyerIdsToCreateNewCarsAndListings =
                        getIdsCollectionToRebalance(true, totalBots, sellerCount);
                if (buyerIdsToCreateNewCarsAndListings.isEmpty()) {
                    return;
                }
                try {
                    botCore.carAndListingCreator(buyerIdsToCreateNewCarsAndListings);
                } catch (Exception e) {
                    return;
                }
                sellerIds.addAll(buyerIdsToCreateNewCarsAndListings);
                log.info("Rebalancing seller sequence: converted {} buyers to sellers",
                        buyerIdsToCreateNewCarsAndListings.size());
            } else if (buyerRatio < MIN_RATIO_THRESHOLD) {
                List<Long> sellerIdsToDeleteHisCarsAndListings =
                        getIdsCollectionToRebalance(false, totalBots, buyerCount);
                if (sellerIdsToDeleteHisCarsAndListings.isEmpty()) {
                    return;
                }
                try {
                    botCore.carAndListingDeleter(sellerIdsToDeleteHisCarsAndListings);
                } catch (Exception e) {
                    return;
                }
                buyerIds.addAll(sellerIdsToDeleteHisCarsAndListings);
                log.info("Rebalancing buyer sequence: converted {} sellers to buyers",
                        sellerIdsToDeleteHisCarsAndListings.size());
            }
        } finally {
            resizeRatioLock.unlock();
        }
    }

    private void collectionSync() {
        List<Long> buyerIds = botCore.getIds(false);
        List<Long> sellerIds = botCore.getIds(true);
        this.buyerIds.clear();
        this.sellerIds.clear();
        for (Long id : buyerIds) {
            if (!inFlightBots.contains(id)) {
                this.buyerIds.addLast(id);
            }
        }
        for (Long id : sellerIds) {
            if (!inFlightBots.contains(id)) {
                this.sellerIds.addLast(id);
            }
        }
    }

    private List<Long> getIdsCollectionToRebalance(boolean isSeller, int totalBots, int currCount) {
        int targetAmount = (int) (totalBots * TARGET_RATIO);
        int toConvert = targetAmount - currCount;
        if (toConvert <= 0) return List.of();
        List<Long> resultIds = new ArrayList<>(toConvert);
        for (int i = 0; i < toConvert; i++) {
            Long id;
            if (isSeller) id = buyerIds.pollFirst();
            else id = sellerIds.pollFirst();
            if (id != null) {
                resultIds.add(id);
            }
        }
        return resultIds;
    }

    private void threadStarter(int numberOfWorkerThreads) {
        collectionSync();
        isBotsRunning = true;
        currentWorkersAmount = numberOfWorkerThreads;
        threadPool = Executors.newVirtualThreadPerTaskExecutor();
        for (int i = 0; i < numberOfWorkerThreads; i++) {
            threadPool.execute(this::runSimulationLoop);
        }
    }

    @Override
    public synchronized void close() {
        isBotsRunning = false;
        currentWorkersAmount = 0;
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                threadPool = null;
            }
        }
    }
}
