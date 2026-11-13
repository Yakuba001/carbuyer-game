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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class BotDistributeServiceIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BotDistributeServiceIT.class);
    @Autowired
    private BotDistributeService distributeService;
    @Autowired
    private BotService botService;
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
        jdbcTemplate.getJdbcTemplate().update("UPDATE bot_config SET status = 'NOT_ADDED' WHERE id = 1");
    }

    @Test
    void startBots_shouldGoWellAndAvoidDeadLocks_whenScenarioIsHappyPath() {
        int startBotAmount = 11;
        int expectedHalf = startBotAmount / 2;
        ReflectionTestUtils.setField(botService, "START_FIX_BOTS_AMOUNT", startBotAmount);
        distributeService.startBots(10);

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        distributeService.stopBots();

        List<Long> resultBuyerList = userFacade.getAllBuyerBotIds();
        List<Long> resultSellerList = userFacade.getAllSellerBotIds();

        assertThat(resultSellerList.size() + resultBuyerList.size()).isEqualTo(startBotAmount);
        assertThat(resultSellerList.size()).isCloseTo(expectedHalf, within(1));
        assertThat(resultBuyerList.size()).isCloseTo(expectedHalf, within(1));
    }

    @Test
    void startBots_shouldWorkCorrectly_whenExecutedConcurrently() {
        int startBotAmount = 5;
        int expectedHalf = startBotAmount / 2;
        ReflectionTestUtils.setField(botService, "START_FIX_BOTS_AMOUNT", startBotAmount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> distributeService.startBots(5));
            }
        }
        distributeService.stopBots();

        List<Long> resultBuyerList = userFacade.getAllBuyerBotIds();
        List<Long> resultSellerList = userFacade.getAllSellerBotIds();

        assertThat(resultSellerList.size()).isCloseTo(expectedHalf, within(1));
        assertThat(resultBuyerList.size()).isCloseTo(expectedHalf, within(1));
    }

    @Test
    void startBots_shouldRebalancingCollections_whenRatioIsNotCorrect() {
        int startBotAmount = 6;
        int expectedHalf = startBotAmount / 2;
        ReflectionTestUtils.setField(botService, "START_FIX_BOTS_AMOUNT", startBotAmount);
        long userId;
        String username = "Bob";
        String email = "bob@g.com";
        String password = "password";
        int maxSlots = 5;
        BigDecimal userBalance = new BigDecimal("1000000");

        try {
            distributeService.startBots(2);
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
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            distributeService.stopBots();
        }

        List<Long> resultBuyerList = userFacade.getAllBuyerBotIds();
        List<Long> resultSellerList = userFacade.getAllSellerBotIds();

        assertThat(resultSellerList.size()).isCloseTo(expectedHalf, within(1));
        assertThat(resultBuyerList.size()).isCloseTo(expectedHalf, within(1));
    }
}
