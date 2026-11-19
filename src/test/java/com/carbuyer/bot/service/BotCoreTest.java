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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    @Spy
    @InjectMocks
    private BotCore botCore;

    private static final Long TEST_SELLER_ID = 1L;
    private static final Long TEST_BUYER_ID = 999L;
    private static final UUID TEST_LISTING_ID = UUID.randomUUID();
    private static final UUID TEST_CAR_ID = UUID.randomUUID();

    @Test
    void addBots_shouldEngageWithUserCarGarageMarketFacadesAndSetHalfSizeOfAllBotsInSellersList_whenEverythingWell() {
        int botCount = 6;
        List<Long> botIds = List.of(1L, 2L, 3L, 4L, 5L, 6L);
        List<Long> expectedSellers = List.of(1L, 2L, 3L);
        List<UUID> carIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(userFacade.addBots(anyInt())).thenReturn(botIds);
        when(carFacade.addCarsByIds(anyList())).thenReturn(carIds);

        botCore.addBots(botCount);

        verify(userFacade).addBots(anyInt());
        verify(carFacade).addCarsByIds(expectedSellers);
        verify(garageFacade).changeEmptyGarageSlotStatusAndSetInItCarId(expectedSellers, carIds);
        verify(marketFacade).createListings(expectedSellers, carIds);
    }

    @Test
    void addBots_shouldDoNothing_whenCountIsLessThanTwo() {
        int botCount = 1;
        botCore.addBots(botCount);

        verifyNoInteractions(userFacade, carFacade, garageFacade, marketFacade);
    }

    @Test
    void addBots_shouldAvoidEngageWithCarMarketAndGarageFacades_whenResultSellerCollectionIsEmpty() {
        int botCount = 2;
        List<Long> botIds = List.of(1L);
        when(userFacade.addBots(anyInt())).thenReturn(botIds);

        botCore.addBots(botCount);

        verify(userFacade).addBots(botCount);
        verifyNoInteractions(carFacade, garageFacade, marketFacade);
    }

    @Test
    void syncBotsCountInDb_shouldAddedSomeNewBots_whenCurrTotalLessThanInProperty() {
        int botCount = 6;
        ReflectionTestUtils.setField(botCore, "startFixBotsAmount", botCount);
        List<Long> expectedSellerIds = List.of(1L, 2L);
        List<Long> expectedBuyerIds = List.of(4L, 5L);
        int currBotExists = expectedSellerIds.size() + expectedBuyerIds.size();
        when(userFacade.getAllSellerBotIds()).thenReturn(expectedSellerIds);
        when(userFacade.getAllBuyerBotIds()).thenReturn(expectedBuyerIds);

        botCore.syncBotsCountInDb();

        verify(botCore).addBots(botCount - currBotExists);
        verify(botCore, never()).carAndListingDeleter(anyList());
    }

    @Test
    void syncBotsCountInDb_shouldDeletedCarsAndListings_whenCurrTotalHigherThanInProperty() {
        int botCount = 2;
        ReflectionTestUtils.setField(botCore, "startFixBotsAmount", botCount);
        List<Long> expectedSellerIds = List.of(1L, 2L);
        List<Long> expectedBuyerIds = List.of(4L, 5L);
        when(userFacade.getAllSellerBotIds()).thenReturn(expectedSellerIds);
        when(userFacade.getAllBuyerBotIds()).thenReturn(expectedBuyerIds);

        botCore.syncBotsCountInDb();

        verify(botCore, never()).addBots(anyInt());
        verify(botCore).carAndListingDeleter(expectedSellerIds.reversed());
    }

    @Test
    void syncBotsCountInDb_shouldDoNothing_whenCurrTotalHigherThanInPropertyButSellersAreNotExists() {
        int botCount = 2;
        ReflectionTestUtils.setField(botCore, "startFixBotsAmount", botCount);
        List<Long> expectedSellerIds = List.of(1L, 2L);
        List<Long> expectedBuyerIds = List.of();
        when(userFacade.getAllBuyerBotIds()).thenReturn(expectedBuyerIds);
        when(userFacade.getAllSellerBotIds()).thenReturn(expectedSellerIds);

        botCore.syncBotsCountInDb();

        verify(botCore, never()).addBots(anyInt());
        verify(botCore, never()).carAndListingDeleter(anyList());
    }

    @Test
    void syncBotsCountInDb_shouldDoNothing_whenAllBuyersAreActuallyNull() {
        List<Long> expectedSellerIds = List.of(1L, 2L);
        when(userFacade.getAllBuyerBotIds()).thenReturn(null);
        when(userFacade.getAllSellerBotIds()).thenReturn(expectedSellerIds);

        botCore.syncBotsCountInDb();

        verify(botCore, never()).addBots(anyInt());
        verify(botCore, never()).carAndListingDeleter(anyList());
    }

    @Test
    void syncBotsCountInDb_shouldDoNothing_whenAllSellersAreActuallyNull() {
        List<Long> expectedBuyerIds = List.of(4L, 5L);
        when(userFacade.getAllBuyerBotIds()).thenReturn(expectedBuyerIds);
        when(userFacade.getAllSellerBotIds()).thenReturn(null);

        botCore.syncBotsCountInDb();

        verify(botCore, never()).addBots(anyInt());
        verify(botCore, never()).carAndListingDeleter(anyList());
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
        when(userFacade.getLimitedSellerBotIds(anyInt())).thenReturn(sellerList);

        List<Long> result = botCore.getIds(true);

        assertThat(result).isEqualTo(sellerList);
        verify(userFacade, never()).getAllBuyerBotIds();
    }

    @Test
    void getIds_shouldGetBuyerListFromDb_whenArgumentEqualFalse() {
        List<Long> buyerList = List.of(1L, 2L, 3L);
        when(userFacade.getLimitedBuyerBotIds(anyInt())).thenReturn(buyerList);

        List<Long> result = botCore.getIds(false);

        assertThat(result).isEqualTo(buyerList);
        verify(userFacade, never()).getAllSellerBotIds();
    }
}
