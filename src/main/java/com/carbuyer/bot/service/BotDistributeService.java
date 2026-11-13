package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.bot.dto.StartBotResponse;
import com.carbuyer.bot.entity.ConfigStatus;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.BotCreationException;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotDistributeService implements AutoCloseable {

    private final BotService botService;
    private final UserFacade userFacade;
    private final MarketFacade marketFacade;
    private final CarFacade carFacade;
    private final GarageFacade garageFacade;

    private ExecutorService threadPool;
    private volatile boolean isBotsRunning = false;
    private final Deque<Long> buyerIds = new ConcurrentLinkedDeque<>();
    private final Deque<Long> sellerIds = new ConcurrentLinkedDeque<>();
    @Value("${app.bot.think-time.min:1000}")
    private Duration minThinkTime;
    @Value("${app.bot.think-time.max:5000}")
    private Duration maxThinkTime;
    private final ReentrantLock resizeRatioLock = new ReentrantLock();
    private static final double MIN_RATIO_THRESHOLD = 0.30;
    private static final double TARGET_RATIO = 0.50;
    private final TransactionTemplate txTemplate;
    private final Set<Long> inFlightBots = ConcurrentHashMap.newKeySet();
    private final AtomicInteger iterationCounter =  new AtomicInteger(0);
    private static final int SYNC_EVERY_N_ITERATIONS = 10;

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
    // TODO: Refactor this, and thinking about netherlands auction to avoid founds starvation in infinity cycle...
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
                    // TODO: transfer this tx to BotService, this give me chance to avoid self-invocation problem and clean code
                    PurchaseResult result = txTemplate.execute(status -> {
                        UUID listingId = marketFacade.getRandomListingId();
                        if (listingId == null) {
                            log.info("ListingId is empty");
                            return null;
                        }
                        UUID listingCarId = marketFacade.getListingCarId(listingId);
                        if (listingCarId == null) {
                            log.info("ListingCarId is empty");
                            return null;
                        }
                        Long sellerId = marketFacade.getBotSellerId(listingId);
                        marketFacade.buyCar(buyerId, listingId);
                        return new PurchaseResult(listingId, listingCarId, sellerId);
                    });
                    if (result == null) {
                        inFlightBots.remove(buyerId);
                        buyerIds.addLast(buyerId);
                        tryRebalance(true);
                        continue;
                    }
                    UUID listingId = result.listingId();
                    UUID listingCarId = result.carId();
                    Long sellerId = result.sellerId();
                    marketFacade.createBotListing(buyerId, listingCarId, priceGenerator());
                    this.sellerIds.addLast(buyerId);
                    if (sellerId != null) {
                        this.sellerIds.remove(sellerId);
                        this.buyerIds.addLast(sellerId);
                        log.info("Bot {} bought a car from Bot {} (Listing: {})", buyerId, sellerId, listingId);
                    }
                    log.info("New listing is created");
                    boolean forceSync = iterationCounter.incrementAndGet() % SYNC_EVERY_N_ITERATIONS == 0;
                    tryRebalance(forceSync);
                } catch (Exception e) {
                    // TODO: thinking about it, because bot buyer can be with car, but its not destroy logic all in all
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
    // TODO: thinking about third hands where user can at the same moment just destroy this method at all and all logic
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
                int targetSellers = (int) (totalBots * TARGET_RATIO);
                int toConvert = targetSellers - sellerCount;
                if (toConvert <= 0) {
                    return;
                }
                List<Long> buyerIdsToCreateNewCarsAndListings = new ArrayList<>(toConvert);
                for (int i = 0; i < toConvert; i++) {
                    Long id = buyerIds.pollFirst();
                    if (id != null) {
                        buyerIdsToCreateNewCarsAndListings.add(id);
                    }
                }
                if (buyerIdsToCreateNewCarsAndListings.isEmpty()) {
                    return;
                }
                // TODO: check it in the future, mb tx or something else need here, now its flaky part of code (or not)
                List<UUID> carIds = carFacade.addCarsByIds(buyerIdsToCreateNewCarsAndListings);
                garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(
                        buyerIdsToCreateNewCarsAndListings, carIds);
                marketFacade.createListings(buyerIdsToCreateNewCarsAndListings, carIds);
                sellerIds.addAll(buyerIdsToCreateNewCarsAndListings);
                log.info("Rebalancing seller sequence: converted {} buyers to sellers", buyerIdsToCreateNewCarsAndListings.size());
            } else if (buyerRatio < MIN_RATIO_THRESHOLD) {
                int targetBuyers = (int) (totalBots * TARGET_RATIO);
                int toConvert = targetBuyers - buyerCount;
                if (toConvert <= 0) {
                    return;
                }
                List<Long> sellerIdsToDeleteHisCarsAndListings = new ArrayList<>(toConvert);
                for (int i = 0; i < toConvert; i++) {
                    Long id = sellerIds.pollFirst();
                    if (id != null) {
                        sellerIdsToDeleteHisCarsAndListings.add(id);
                    }
                }
                if (sellerIdsToDeleteHisCarsAndListings.isEmpty()) {
                    return;
                }
                // TODO: check it in the future, mb tx or something else need here, now its flaky part of code (or not)
                marketFacade.deleteListingsBySellerIds(sellerIdsToDeleteHisCarsAndListings);
                carFacade.deleteCarsByOwnerIds(sellerIdsToDeleteHisCarsAndListings);
                garageFacade.changeGarageStatusesFromOccupiedToEmptyByUserIds(
                        sellerIdsToDeleteHisCarsAndListings);
                buyerIds.addAll(sellerIdsToDeleteHisCarsAndListings);
                log.info("Rebalancing buyer sequence: converted {} sellers to buyers", sellerIdsToDeleteHisCarsAndListings.size());
            }
        } finally {
            resizeRatioLock.unlock();
        }
    }

    private void collectionSync() {
        List<Long> buyerIds = userFacade.getAllBuyerBotIds();
        List<Long> sellerIds = userFacade.getAllSellerBotIds();
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

    private BigDecimal priceGenerator() {
        double lowerBound = 500.00;
        double upperBound = 1000.00;
        return new BigDecimal(ThreadLocalRandom.current().nextDouble(lowerBound, upperBound))
                .setScale(2, RoundingMode.HALF_UP);
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
}
