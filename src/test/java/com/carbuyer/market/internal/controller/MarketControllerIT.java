package com.carbuyer.market.internal.controller;

import com.carbuyer.BaseIntegrationTest;
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

public class MarketControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager em;

    @Test
    @Transactional
    @WithMockUser(username = "Bob")
    void createListing_shouldReturnCreatedStatusAndSaveToDbListing() throws Exception {
        long sellerId = 1L;
        CreateListingRequestDto requestDto = insertUserAndCarToDb(sellerId);

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
    @Transactional
    @WithMockUser(username = "Bob")
    void cancelListing_shouldChangeStatusFromActiveToCancelledAndSetClosedAtTime() throws Exception {
        long sellerId = 1L;
        UUID listingId = UUID.randomUUID();
        insertListing(insertUserAndCarToDb(sellerId), sellerId, listingId);

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
    @Transactional
    @WithMockUser(username = "Bob")
    void getListings_shouldReturnPageWithAllActiveListings() throws Exception {
        long sellerId = 1L;
        UUID firstListingId = UUID.randomUUID();
        UUID secondListingId = UUID.randomUUID();
        UUID thirdListingId = UUID.randomUUID();
        CreateListingRequestDto requestInsertUserAndCar = insertUserAndCarToDb(sellerId);
        insertListing(requestInsertUserAndCar, sellerId, firstListingId);
        insertListing(insertCarToDb(sellerId), sellerId, secondListingId);
        insertListing(insertCarToDb(sellerId), sellerId, thirdListingId);

        em.flush();
        em.clear();

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

    private void insertListing(CreateListingRequestDto requestDto, long sellerId, UUID listingId) {
        UUID carId = requestDto.carId();
        BigDecimal price = requestDto.price();
        jdbcTemplate.update(
                "INSERT INTO market_listings (id, car_id, seller_id, price) OVERRIDING SYSTEM VALUE " +
                        "VALUES (?, ?, ?, ?)", listingId, carId, sellerId, price);
    }

    private CreateListingRequestDto insertCarToDb(long sellerId) {
        UUID carId = UUID.randomUUID();
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        BigDecimal listingPrice = new BigDecimal("12000.00");

        jdbcTemplate.update(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                carId, randomVin, "Toyota Camry", 50000, 90, new BigDecimal("14000.00"), sellerId
        );
        return new CreateListingRequestDto(carId, listingPrice);
    }

    private CreateListingRequestDto insertUserAndCarToDb(long sellerId) {
        String email = "bob@gmail.com";

        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash) " +
                        "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?)",
                sellerId, "Bob", email, "secretPass"
        );
        CreateListingRequestDto request = insertCarToDb(sellerId);
        return new CreateListingRequestDto(request.carId(), request.price());
    }
}
