package com.carbuyer.bot.service;

import com.carbuyer.bot.dto.PurchaseResult;
import com.carbuyer.car.api.CarFacade;
import com.carbuyer.exception.exceptions.InsufficientFundsException;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BotCoreTest {

    @Mock
    private UserFacade userFacade;
    @Mock
    private CarFacade carFacade;
    @Mock
    private GarageFacade garageFacade;
    @Mock
    private MarketFacade marketFacade;
    @InjectMocks
    private BotCore botCore;

    private static final Long TEST_SELLER_ID = 1L;
    private static final Long TEST_BUYER_ID = 999L;
    private static final UUID TEST_LISTING_ID = UUID.randomUUID();
    private static final UUID TEST_CAR_ID = UUID.randomUUID();

    @Test
    void addBots_shouldEngageWithUserCarGarageMarketFacadesAndSetHalfSizeOfAllBotsInSellersList_whenEverythingWell() {
        List<Long> botIds = List.of(1L, 2L, 3L, 4L, 5L, 6L);
        List<Long> expectedSellers = List.of(1L, 2L, 3L);
        List<UUID> carIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(userFacade.addBots(anyInt())).thenReturn(botIds);
        when(carFacade.addCarsByIds(anyList())).thenReturn(carIds);

        botCore.addBots();

        verify(userFacade).addBots(anyInt());
        verify(carFacade).addCarsByIds(expectedSellers);
        verify(garageFacade).changeEmptyGarageSlotStatusAndSetInItCarId(expectedSellers, carIds);
        verify(marketFacade).createListings(expectedSellers, carIds);
    }

    @Test
    void executeBotCarPurchase_shouldReturnCorrectPurchaseResult_whenEverythingWentWell() {
        when(marketFacade.getRandomListingId()).thenReturn(TEST_LISTING_ID);
        when(marketFacade.getListingCarId(any(UUID.class))).thenReturn(TEST_CAR_ID);
        when(marketFacade.getBotSellerId(any(UUID.class))).thenReturn(TEST_SELLER_ID);
        PurchaseResult expectedResult = new PurchaseResult(TEST_LISTING_ID, TEST_CAR_ID, TEST_SELLER_ID);

        PurchaseResult result = botCore.executeBotCarPurchase(TEST_BUYER_ID);

        assertThat(result).isEqualTo(expectedResult);

        verify(marketFacade).getRandomListingId();
        verify(marketFacade).getListingCarId(TEST_LISTING_ID);
        verify(marketFacade).getBotSellerId(TEST_LISTING_ID);
        verify(marketFacade).buyCar(TEST_BUYER_ID, TEST_LISTING_ID);
        verify(marketFacade, never()).decreaseListingPrice(TEST_LISTING_ID);
    }

    @Test
    void executeBotCarPurchase_shouldReturnNull_whenListingIdIsNull() {
        PurchaseResult result = botCore.executeBotCarPurchase(TEST_BUYER_ID);

        assertThat(result).isNull();
    }

    @Test
    void executeBotCarPurchase_shouldReturnNull_whenCarIdIsNull() {
        when(marketFacade.getRandomListingId()).thenReturn(TEST_LISTING_ID);

        PurchaseResult result = botCore.executeBotCarPurchase(TEST_BUYER_ID);

        assertThat(result).isNull();
    }

    @Test
    void executeBotCarPurchase_shouldInvokeDecreaseListingPriceAndReturnNull_whenInsufficientFundsExceptionThrows() {
        InsufficientFundsException exception = new InsufficientFundsException("", TEST_LISTING_ID);
        when(marketFacade.getRandomListingId()).thenReturn(TEST_LISTING_ID);
        when(marketFacade.getListingCarId(any(UUID.class))).thenReturn(TEST_CAR_ID);
        when(marketFacade.getBotSellerId(any(UUID.class))).thenReturn(TEST_SELLER_ID);
        doThrow(exception).when(marketFacade).buyCar(TEST_BUYER_ID, TEST_LISTING_ID);

        PurchaseResult result = botCore.executeBotCarPurchase(TEST_BUYER_ID);

        assertThat(result).isNull();
        verify(marketFacade).decreaseListingPrice(TEST_LISTING_ID);
    }

    @Test
    void getIds_shouldGetSellerListFromDb_whenArgumentEqualTrue() {
        List<Long> sellerList = List.of(1L, 2L, 3L);
        when(userFacade.getAllSellerBotIds()).thenReturn(sellerList);

        List<Long> result = botCore.getIds(true);

        assertThat(result).isEqualTo(sellerList);
        verify(userFacade, never()).getAllBuyerBotIds();
    }

    @Test
    void getIds_shouldGetBuyerListFromDb_whenArgumentEqualFalse() {
        List<Long> buyerList = List.of(1L, 2L, 3L);
        when(userFacade.getAllBuyerBotIds()).thenReturn(buyerList);

        List<Long> result = botCore.getIds(false);

        assertThat(result).isEqualTo(buyerList);
        verify(userFacade, never()).getAllSellerBotIds();
    }
}
