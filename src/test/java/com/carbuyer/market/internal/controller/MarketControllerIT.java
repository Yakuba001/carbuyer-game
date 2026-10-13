package com.carbuyer.market.internal.controller;

import com.carbuyer.market.api.dto.CreateListingRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class MarketControllerIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

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
        Long sellerId = 1L;
        String email = "bob@gmail.com";
        UUID carId = UUID.randomUUID();
        String randomVin = UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        BigDecimal listingPrice = new BigDecimal("12000.00");

        jdbcTemplate.update(
                "INSERT INTO users (id, name, email, password_hash) " +
                        "OVERRIDING SYSTEM VALUE VALUES (?, ?, ?, ?)",
                sellerId, "Bob", email, "secretPass"
        );
        jdbcTemplate.update(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                carId, randomVin, "Toyota Camry", 50000, 90, new BigDecimal("14000.00"), sellerId
        );
        CreateListingRequestDto requestDto = new CreateListingRequestDto(carId, listingPrice);

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
}
