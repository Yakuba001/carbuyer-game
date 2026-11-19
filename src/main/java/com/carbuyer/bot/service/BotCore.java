package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.InsufficientFundsException;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class BotCore {

    @Value("${app.bot.initial-size:1000}")
    private int startFixBotsAmount;

    private final UserFacade userFacade;
    private final CarFacade carFacade;
    private final GarageFacade garageFacade;
    private final MarketFacade marketFacade;

    public void addBots(int count) {
        if (count < 2) return;
        List<Long> allAddedBotIds = userFacade.addBots(count);
        List<Long> sellers = new ArrayList<>(allAddedBotIds.subList(0, allAddedBotIds.size() / 2));
        if (!sellers.isEmpty()) {
            List<UUID> carIds = carFacade.addCarsByIds(sellers);
            garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(sellers, carIds);
            marketFacade.createListings(sellers, carIds);
        }
    }

    @Transactional
    public void syncBotsCountInDb() {
        List<Long> allBuyers = userFacade.getAllBuyerBotIds();
        List<Long> allSellers = userFacade.getAllSellerBotIds();
        if (allBuyers == null || allSellers == null) return;
        int currentTotal = allBuyers.size() + allSellers.size();
        if (currentTotal < startFixBotsAmount) {
            int botsToCreate = startFixBotsAmount - currentTotal;
            addBots(botsToCreate);
        } else if (currentTotal > startFixBotsAmount) {
            int excessCount = currentTotal - startFixBotsAmount;
            List<Long> sellersToDeactivate = allSellers.stream()
                    .sorted(Comparator.reverseOrder())
                    .limit(excessCount)
                    .toList();
            if (!sellersToDeactivate.isEmpty()) {
                carAndListingDeleter(sellersToDeactivate);
            }
        }
    }

    public PurchaseResult executeBotCarPurchase(Long buyerId) {
        UUID listingId = marketFacade.getRandomListingId();
        if (listingId == null) {
            return null;
        }
        UUID listingCarId = marketFacade.getListingCarId(listingId);
        if (listingCarId == null) {
            return null;
        }
        Long sellerId = marketFacade.getBotSellerId(listingId);
        try {
            marketFacade.buyCar(buyerId, listingId);
            return new PurchaseResult(listingId, listingCarId, sellerId);
        } catch (InsufficientFundsException e) {
            if (e.getListingId() != null) {
                marketFacade.decreaseListingPrice(e.getListingId());
            }
            return null;
        }
    }

    public void listingCreator(Long buyerId, UUID listingCarId) {
        marketFacade.createBotListing(buyerId, listingCarId, priceGenerator());
    }

    public void carAndListingCreator(List<Long> inputIds) {
        List<UUID> carIds = carFacade.addCarsByIds(inputIds);
        garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(inputIds, carIds);
        marketFacade.createListings(inputIds, carIds);
    }

    public void carAndListingDeleter(List<Long> inputIds) {
        marketFacade.deleteListingsBySellerIds(inputIds);
        carFacade.deleteCarsByOwnerIds(inputIds);
        garageFacade.changeGarageStatusesFromOccupiedToEmptyByUserIds(inputIds);
    }

    @Transactional(readOnly = true)
    public List<Long> getIds(boolean isSeller) {
        if (isSeller) return userFacade.getLimitedSellerBotIds(startFixBotsAmount);
        else return userFacade.getLimitedBuyerBotIds(startFixBotsAmount);
    }

    private BigDecimal priceGenerator() {
        double lowerBound = 500.00;
        double upperBound = 1000.00;
        return new BigDecimal(ThreadLocalRandom.current().nextDouble(lowerBound, upperBound))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
