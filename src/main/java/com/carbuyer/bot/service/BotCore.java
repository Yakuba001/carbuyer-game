package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class BotCore {

    private final static int START_FIX_BOTS_AMOUNT = 1000;

    private final UserFacade userFacade;
    private final CarFacade carFacade;
    private final GarageFacade garageFacade;
    private final MarketFacade marketFacade;

    public void addBots() {
        List<Long> allAddedBotIds = userFacade.addBots(START_FIX_BOTS_AMOUNT);
        List<Long> sellers = new ArrayList<>(allAddedBotIds.subList(0, allAddedBotIds.size() / 2));
        List<UUID> carIds = carFacade.addCarsByIds(sellers);
        garageFacade.changeEmptyGarageSlotStatusAndSetInItCarId(sellers, carIds);
        marketFacade.createListings(sellers, carIds);
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
        marketFacade.buyCar(buyerId, listingId);
        return new PurchaseResult(listingId, listingCarId, sellerId);
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
        if (isSeller) return userFacade.getAllSellerBotIds();
        else return userFacade.getAllBuyerBotIds();
    }

    private BigDecimal priceGenerator() {
        double lowerBound = 500.00;
        double upperBound = 1000.00;
        return new BigDecimal(ThreadLocalRandom.current().nextDouble(lowerBound, upperBound))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
