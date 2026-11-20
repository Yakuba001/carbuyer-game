package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.dto.StopBotResponse;
import com.carbuyer.bot.entity.BotStatus;
import com.carbuyer.bot.util.ConcurrentUniqueQueue;
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

    private final ConcurrentUniqueQueue<Long> buyerIds = new ConcurrentUniqueQueue<>();
    private final ConcurrentUniqueQueue<Long> sellerIds = new ConcurrentUniqueQueue<>();
    private final Set<Long> inFlightBots = ConcurrentHashMap.newKeySet();

    @Value("${app.bot.think-time.min:1000}")
    private Duration minThinkTime;
    @Value("${app.bot.think-time.max:5000}")
    private Duration maxThinkTime;
    private static final double MIN_RATIO_THRESHOLD = 0.30;
    private static final double TARGET_RATIO = 0.50;
    @Value("${app.bot.initial-size}")
    private int syncEveryNIterations;

    private final ReentrantLock resizeRatioLock = new ReentrantLock();
    private final AtomicInteger iterationCounter = new AtomicInteger(0);
    private CountDownLatch stopLatch = new CountDownLatch(1);

    public synchronized StartBotResponse startBots(int numberOfWorkerThreads) {
        if (isBotsRunning) {
            return new StartBotResponse(BotStatus.ALREADY_STARTED.toString(), currentWorkersAmount);
        }
        if (numberOfWorkerThreads <= 0) {
            throw new InvalidBotAmountException("Number of worker threads must be greater than 0");
        }
        try {
            botCore.syncBotsCountInDb();
            collectionSync();
            int totalBotsInSys = buyerIds.size() + sellerIds.size();
            if (totalBotsInSys == 0) {
                throw new StartBotsException("No bots available in db to start simulation");
            }
            int maxSafeWorkers = Math.max(1, (int) (totalBotsInSys * TARGET_RATIO));
            int effectiveWorkers = Math.min(numberOfWorkerThreads, maxSafeWorkers);
            if (effectiveWorkers < numberOfWorkerThreads) {
                log.warn("Requested {} worker threads, but capped to {} (50% of {} total bots) to prevent starvation",
                        numberOfWorkerThreads, effectiveWorkers, totalBotsInSys);
            }
            isBotsRunning = true;
            currentWorkersAmount = effectiveWorkers;
            threadPool = Executors.newVirtualThreadPerTaskExecutor();

            CountDownLatch currentLatch = new CountDownLatch(1);
            this.stopLatch = currentLatch;

            for (int i = 0; i < effectiveWorkers; i++) {
                threadPool.execute(() -> runSimulationLoop(currentLatch));
            }
            return new StartBotResponse(BotStatus.SUCCESSFULLY_STARTED.toString(), currentWorkersAmount);
        } catch (Exception e) {
            stopBots();
            if (e instanceof StartBotsException sbe) {
                throw sbe;
            }
            throw new StartBotsException("An unexpected error occurred while starting bots", e);
        }
    }

    public synchronized StopBotResponse stopBots() {
        close();
        return new StopBotResponse(BotStatus.STOPPED.toString());
    }

    private void runSimulationLoop(CountDownLatch currentLatch) {
        while (isBotsRunning && !Thread.currentThread().isInterrupted()) {
            try {
                long minMs = minThinkTime.toMillis();
                long maxMs = maxThinkTime.toMillis();
                long thinkTimeMs = (minMs >= maxMs) ? minMs : ThreadLocalRandom.current().nextLong(minMs, maxMs);
                if (currentLatch.await(thinkTimeMs, TimeUnit.MILLISECONDS)) {
                    break;
                }
                Long buyerId = buyerIds.pollFirst();
                if (buyerId == null) {
                    tryRebalance(true);
                    if (currentLatch.await(100, TimeUnit.MILLISECONDS)) break;
                    continue;
                }
                inFlightBots.add(buyerId);
                try {
                    PurchaseResult purchaseResult = botCore.executeBotCarPurchase(buyerId);
                    if (purchaseResult == null) {
                        inFlightBots.remove(buyerId);
                        buyerIds.addLast(buyerId);
                        tryRebalance(true);
                        if (currentLatch.await(100, TimeUnit.MILLISECONDS)) break;
                        continue;
                    }
                    UUID listingCarId = purchaseResult.carId();
                    Long sellerId = purchaseResult.sellerId();
                    if (sellerId != null && this.sellerIds.remove(sellerId)) {
                        this.buyerIds.addLast(sellerId);
                        log.info("Bot {} bought a car from Bot {}", buyerId, sellerId);
                    }
                    botCore.listingCreator(buyerId, listingCarId);
                    this.sellerIds.addLast(buyerId);
                    int syncInterval = Math.max(100, syncEveryNIterations);
                    boolean forceSync = iterationCounter.incrementAndGet() % syncInterval == 0;
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
            if (sellerRatio < MIN_RATIO_THRESHOLD) {
                List<Long> buyerIdsToCreate = getIdsCollectionToRebalance(true, totalBots, sellerCount);
                if (buyerIdsToCreate.isEmpty()) {
                    return;
                }
                try {
                    botCore.carAndListingCreator(buyerIdsToCreate);
                    sellerIds.addAll(buyerIdsToCreate);
                    log.info("Rebalancing seller sequence: converted {} buyers to sellers", buyerIdsToCreate.size());
                } catch (Exception e) {
                    buyerIds.addAll(buyerIdsToCreate);
                    log.warn("Failed to rebalance, returned {} bots to buyer queue", buyerIdsToCreate.size(), e);
                }
            } else if (buyerRatio < MIN_RATIO_THRESHOLD) {
                List<Long> sellerIdsToDelete = getIdsCollectionToRebalance(false, totalBots, buyerCount);
                if (sellerIdsToDelete.isEmpty()) {
                    return;
                }
                try {
                    botCore.carAndListingDeleter(sellerIdsToDelete);
                    buyerIds.addAll(sellerIdsToDelete);
                    log.info("Rebalancing buyer sequence: converted {} sellers to buyers", sellerIdsToDelete.size());
                } catch (Exception e) {
                    sellerIds.addAll(sellerIdsToDelete);
                    log.warn("Failed to rebalance, returned {} bots to seller queue", sellerIdsToDelete.size(), e);
                }
            }
        } finally {
            resizeRatioLock.unlock();
        }
    }

    private void collectionSync() {
        Set<Long> freshBuyers = new HashSet<>(botCore.getIds(false));
        Set<Long> freshSellers = new HashSet<>(botCore.getIds(true));
        freshBuyers.removeAll(inFlightBots);
        freshSellers.removeAll(inFlightBots);
        this.buyerIds.syncWith(freshBuyers, inFlightBots);
        this.sellerIds.syncWith(freshSellers, inFlightBots);
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

    @Override
    public synchronized void close() {
        isBotsRunning = false;
        currentWorkersAmount = 0;
        if (threadPool != null) {
            stopLatch.countDown();
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
