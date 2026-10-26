package com.carbuyer.market.internal.controller;

import com.carbuyer.containers.MockMvcIntegrationTest;
import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class MarketControllerIT extends MockMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager em;

    private static final UUID TEST_CAR_ID = UUID.randomUUID();
    private static final BigDecimal TEST_PRICE = new BigDecimal("10");

    @Test
    @WithMockUser(username = "Bob")
    void createListing_shouldReturnCreatedStatusAndSaveToDbListing() throws Exception {
        long sellerId = 1L;
        addUserToDb(sellerId);
        addCarToDb(sellerId, TEST_CAR_ID);
        CreateListingRequestDto requestDto = new CreateListingRequestDto(TEST_CAR_ID, TEST_PRICE);

        mockMvc.perform(post("/api/v1/market/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());

        em.flush();

        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM market_listings WHERE seller_id = ?",
                Integer.class, sellerId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "Bob")
    void createListing_shouldReturnBadRequest_whenSendInvalidRequest() throws Exception {
        CreateListingRequestDto requestDto = new CreateListingRequestDto(null, null);

        mockMvc.perform(post("/api/v1/market/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createListing_shouldReturnForbitten_whenUserWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/market/listings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "Bob")
    void cancelListing_shouldChangeStatusFromActiveToCancelledAndSetClosedAtTime() throws Exception {
        long sellerId = 1L;
        UUID listingId = UUID.randomUUID();
        addUserToDb(sellerId);
        addCarToDb(sellerId, listingId);
        insertListing(new CreateListingRequestDto(listingId, TEST_PRICE), sellerId, listingId);

        mockMvc.perform(post("/api/v1/market/listings/{id}/cancel", listingId))
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();

        Map<String, Object> result =
                jdbcTemplate.queryForMap("SELECT status, closed_at FROM market_listings WHERE id = ?", listingId);

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result.get("status")).isEqualTo("CANCELLED");
        assertThat(result.get("closed_at")).isNotNull();
    }

    @Test
    @WithMockUser(username = "Bob")
    void cancelListing_shouldReturnNotFound_whenListingDoesNotExistInDb() throws Exception {
        mockMvc.perform(post("/api/v1/market/listings/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelListing_shouldReturnForbitten_whenUserWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/market/listings/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "Bob")
    void getListings_shouldReturnPageWithAllActiveListingsAndStatusOk() throws Exception {
        long sellerId = 1L;
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};

        addUserToDb(sellerId);
        for (UUID uuid : uuids) {
            addCarToDb(sellerId, uuid);
            insertListing(new CreateListingRequestDto(uuid, TEST_PRICE), sellerId, uuid);
        }

        mockMvc.perform(get("/api/v1/market/listings")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").isNotEmpty());
    }

    @Test
    @WithMockUser("John")
    void buyCar_shouldReturnNoContent() throws Exception {
        long sellerId = 1L;
        long buyerId = 2L;
        UUID listingId = UUID.randomUUID();
        insertListing(insertSellerAndBuyerToDb(sellerId, buyerId), sellerId, listingId);

        mockMvc.perform(post("/api/v1/market/listings/{id}/buy", listingId))
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();

        Map<String, Object> resultMap = jdbcTemplate.queryForMap(
                "SELECT status, closed_at, buyer_id FROM market_listings WHERE id = ?", listingId);
        long carOwnerId =
                jdbcTemplate.queryForObject("SELECT owner_id FROM cars WHERE id = ?", Long.class, TEST_CAR_ID);

        assertThat(resultMap).isNotNull();
        assertThat(resultMap.get("status")).isEqualTo("SOLD");
        assertThat(resultMap.get("closed_at")).isNotNull();
        assertThat(resultMap.get("buyer_id")).isEqualTo(buyerId);

        assertThat(carOwnerId).isEqualTo(buyerId);
    }

    @Test
    void buyCar_shouldReturnForbitten_whenUserWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/market/listings/{id}/buy", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    private void insertListing(CreateListingRequestDto requestDto, long sellerId, UUID listingId) {
        UUID carId = requestDto.carId();
        BigDecimal price = requestDto.price();
        jdbcTemplate.update(
                "INSERT INTO market_listings (id, car_id, seller_id, price) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)", listingId, carId, sellerId, price);
    }

    private void addUserToDb(long sellerId) {
        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash) " +
                        "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?)",
                sellerId, "Bob", "bib@g.c", "secretPass");
    }

    private void addCarToDb(long sellerId, UUID carId) {
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        jdbcTemplate.update(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                carId, randomVin, "Toyota Camry", 50000, 90, new BigDecimal("20"), sellerId);
    }

    private CreateListingRequestDto insertSellerAndBuyerToDb(long sellerId, long buyerId) {
        BigDecimal sellerBalance = new BigDecimal("100");
        int slotNumber = 1;
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        BigDecimal listingPrice = new BigDecimal("50");
        BigDecimal buyerBalance = new BigDecimal("1000");

        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash) " +
                        "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?)",
                sellerId, "Bob", "bib@g.c", "secretPass");
        jdbcTemplate.update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", sellerId, sellerBalance);
        jdbcTemplate.update(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                TEST_CAR_ID, randomVin, "Toyota Camry", 50000, 90, new BigDecimal("20"), sellerId);
        jdbcTemplate.update("INSERT INTO garage_slots (user_id, slot_number, car_id, status) VALUES (?, ?, ?, ?)",
                sellerId, slotNumber, TEST_CAR_ID, "OCCUPIED");

        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash) " +
                        "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?)",
                buyerId, "John", "b@g.c", "secretPass");
        jdbcTemplate.update("INSERT INTO user_balances (user_id, balance) VALUES (?, ?)", buyerId, buyerBalance);
        jdbcTemplate.update("INSERT INTO garage_slots (user_id, slot_number) VALUES (?, ?)",
                buyerId, slotNumber);

        return new CreateListingRequestDto(TEST_CAR_ID, listingPrice);
    }
}
