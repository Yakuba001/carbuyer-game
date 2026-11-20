package com.carbuyer.bot.service;

import com.carbuyer.containers.BaseIntegrationTest;
import com.carbuyer.market.api.MarketFacade;
import com.carbuyer.user.api.UserFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class BotServiceIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BotServiceIT.class);
    @Autowired
    private BotService distributeService;
    @Autowired
    private BotCore botCore;
    @Autowired
    private UserFacade userFacade;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private MarketFacade marketFacade;

    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate txTemplate;

    @BeforeEach
    public void setup() {
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    public void tearDown() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    void startBots_shouldBeFlexibleAndEngageCorrectlyWithRealUser_whenInPropertiesChangedNumberOfStartBots() {
        List<Long> resultBuyerList1000, resultSellerList1000,resultBuyerList500, resultSellerList500,
                resultBuyerList1500, resultSellerList1500;
        int sleepTime = 2;
        // 1000 bots
        try {
            distributeService.startBots(Integer.MAX_VALUE);
            TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            distributeService.stopBots();
            resultBuyerList1000 = botCore.getIds(false);
            resultSellerList1000 = botCore.getIds(true);
        }
        // 500 bots
        try {
            ReflectionTestUtils.setField(botCore, "startFixBotsAmount", 500);
            distributeService.startBots(5);
            TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            distributeService.stopBots();
            resultBuyerList500 = botCore.getIds(false);
            resultSellerList500 = botCore.getIds(true);
        }
        // 1500 bots
        try {
            ReflectionTestUtils.setField(botCore, "startFixBotsAmount", 1500);
            distributeService.startBots(Integer.MAX_VALUE);
            // Real user making the deals in concurrency environment
            realUserBuyerSim();
            TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            distributeService.stopBots();
            resultBuyerList1500 = botCore.getIds(false);
            resultSellerList1500 = botCore.getIds(true);
        }

        List<Long> allBuyers = userFacade.getAllBuyerBotIds();
        List<Long> allSellers = userFacade.getAllSellerBotIds();

        assertThat(allBuyers.size() + allSellers.size()).isEqualTo(1500);
        assertThat(resultBuyerList1000.size() + resultSellerList1000.size()).isEqualTo(1000);
        assertThat(resultSellerList500.size() + resultBuyerList500.size()).isEqualTo(500);
        assertThat(resultSellerList1500.size() + resultBuyerList1500.size()).isEqualTo(1500);
    }

    private void realUserBuyerSim() {
        long userId;
        String username = "Bob";
        String email = "bob@g.com";
        String password = "password";
        int maxSlots = 5;
        BigDecimal userBalance = new BigDecimal("1000000");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource userParam = new MapSqlParameterSource();
        userParam.addValue("name", username);
        userParam.addValue("email", email);
        userParam.addValue("password", password);
        userParam.addValue("maxSlots", maxSlots);
        String userInsertSql = "INSERT INTO users (name, email, password_hash, max_slots) " +
                "VALUES (:name, :email, :password, :maxSlots)";
        jdbcTemplate.update(userInsertSql, userParam, keyHolder, new String[]{"id"});
        userId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        jdbcTemplate.getJdbcTemplate()
                .update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", userId, userBalance);
        MapSqlParameterSource[] slotParams = IntStream.range(0, maxSlots)
                .mapToObj(i -> {
                    MapSqlParameterSource param = new MapSqlParameterSource();
                    param.addValue("userId", userId);
                    param.addValue("slotNumber", i);
                    return param;
                }).toArray(MapSqlParameterSource[]::new);
        String sql = "INSERT INTO garage_slots (user_id, slot_number) VALUES (:userId, :slotNumber)";
        jdbcTemplate.batchUpdate(sql, slotParams);
        txTemplate.executeWithoutResult(status -> {
            for (int i = 0; i < 3; i++) {
                UUID currListingId = marketFacade.getRandomListingId();
                if (currListingId == null) {
                    log.warn("listingId is null");
                    break;
                }
                marketFacade.buyCar(userId, currListingId);
            }
        });

        Integer occupiedSlotsCount = jdbcTemplate.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM garage_slots WHERE user_id = ? AND status = 'OCCUPIED' AND car_id IS NOT NULL",
                Integer.class, userId
        );
        List<UUID> boughtListingIds = jdbcTemplate.getJdbcTemplate().queryForList(
                "SELECT id FROM market_listings WHERE buyer_id = ? AND status = 'SOLD'",
                UUID.class, userId
        );

        assertThat(occupiedSlotsCount).isEqualTo(3);
        assertThat(boughtListingIds).hasSize(3);
    }
}
