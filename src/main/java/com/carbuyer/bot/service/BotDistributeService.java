package com.carbuyer.bot.service;

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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
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
        if (isBotsRunning || numberOfWorkerThreads < 4) return;
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

                    // TODO: just a skeleton, thinking about it............ proves non-null and so on...
//                    if (resizeRatioLock.tryLock()) {
//                        try {
//                            int buyerCount = buyerIds.size();
//                            int sellerCount = sellerIds.size();
//                            int totalBots = buyerCount + sellerCount;
//                            double buyerRatio = (double) buyerCount / totalBots;
//                            double sellerRatio = (double) sellerCount / totalBots;
//                            if (sellerRatio < MIN_RATIO_THRESHOLD) {
//                                int targetSellers = (int) (totalBots * TARGET_RATIO);
//                                int toConvert = targetSellers - sellerCount;
//                                List<Long> buyerIdsToCreateNewCarsAndListings = new ArrayList<>(toConvert);
//                                for (int i = 0; i < toConvert; i++) {
//                                    buyerIdsToCreateNewCarsAndListings.add(buyerIds.pollFirst());
//                                }
//                                List<UUID> carIds = carFacade.addCarsByIds(buyerIdsToCreateNewCarsAndListings);
//                                garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(
//                                        buyerIdsToCreateNewCarsAndListings, carIds);
//                                marketFacade.createListings(buyerIdsToCreateNewCarsAndListings, carIds);
//                                sellerIds.addAll(buyerIdsToCreateNewCarsAndListings);
//                            } else if (buyerRatio < MIN_RATIO_THRESHOLD) {
//                                int targetBuyers = (int) (totalBots * TARGET_RATIO);
//                                int toConvert = targetBuyers - buyerCount;
//                                List<Long> sellerIdsToDeleteHisCarsAndListings = new ArrayList<>(toConvert);
//                                for (int i = 0; i < toConvert; i++) {
//                                    sellerIdsToDeleteHisCarsAndListings.add(sellerIds.pollFirst());
//                                }
//                                marketFacade.deleteListingsBySellerIds(sellerIdsToDeleteHisCarsAndListings);
//                                carFacade.deleteCarsByOwnerIds(sellerIdsToDeleteHisCarsAndListings);
//                                garageFacade.changeGarageStatusesFromOccupiedToEmptyByUserIds(
//                                        sellerIdsToDeleteHisCarsAndListings);
//                                buyerIds.addAll(sellerIdsToDeleteHisCarsAndListings);
//                            }
//                        } finally {
//                            resizeRatioLock.unlock();
//                        }
//                    }

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

    private BigDecimal priceGenerator() {
        double lowerBound = 500.00;
        double upperBound = 1000.00;
        return new BigDecimal(ThreadLocalRandom.current().nextDouble(lowerBound, upperBound))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
