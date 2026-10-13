package com.carbuyer.market.internal.repository;

import com.carbuyer.market.internal.entity.ListingStatus;
import com.carbuyer.market.internal.entity.MarketListing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MarketListingRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Autowired
    private MarketListingRepository marketListingRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void findAllByStatus_shouldReturnOnlyActiveListings_andCheckAutoFillingDefaultField() {
        Long sellerId = 1L;
        String sellerName = "Bob";
        String sellerEmail = "bob@g.com";
        String sellerPassword = "bobby123";
        BigDecimal price = new BigDecimal("100");
        UUID firstCarId = UUID.randomUUID();
        UUID secondCarId = UUID.randomUUID();
        UUID thirdCarId = UUID.randomUUID();
        String randomVin1 = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String randomVin2 = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        String randomVin3 = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        jdbcTemplate.execute(String.format(
                "INSERT INTO users (id, name, email, password_hash) OVERRIDING SYSTEM VALUE VALUES ('%d', '%s', '%s', '%s') ON CONFLICT (id) DO NOTHING",
                sellerId, sellerName, sellerEmail, sellerPassword
        ));
        jdbcTemplate.execute(String.format(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES ('%s', '%s', 'Test Model', 10000, 85, 15000.00, %d)",
                firstCarId, randomVin1, sellerId
        ));
        jdbcTemplate.execute(String.format(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES ('%s', '%s', 'Test Model', 10000, 85, 15000.00, %d)",
                secondCarId, randomVin2, sellerId
        ));
        jdbcTemplate.execute(String.format(
                "INSERT INTO cars (id, vin, model, mileage, condition_score, real_price, owner_id) " +
                        "VALUES ('%s', '%s', 'Test Model', 10000, 85, 15000.00, %d)",
                thirdCarId, randomVin3, sellerId
        ));
        MarketListing exampleListing1 = new MarketListing(firstCarId, sellerId, null, price);
        MarketListing exampleListing2 = new MarketListing(secondCarId, sellerId, null, price);
        MarketListing exampleListing3 = new MarketListing(thirdCarId, sellerId, null, price);
        List<MarketListing> listings = List.of(exampleListing1, exampleListing2, exampleListing3);
        marketListingRepository.saveAllAndFlush(listings);
        exampleListing3.setStatus(ListingStatus.CANCELLED);
        exampleListing3.setClosedAt(Instant.now());
        marketListingRepository.flush();
        Pageable pageable = PageRequest.of(0, 1);

        Page<MarketListing> resultPage = marketListingRepository.findAllByStatus(ListingStatus.ACTIVE, pageable);

        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
        assertThat(resultPage.getTotalPages()).isEqualTo(2);
        assertThat(resultPage.getContent().size()).isEqualTo(1);

        MarketListing firstElement = resultPage.getContent().getFirst();
        assertThat(firstElement.getId()).isNotNull();
        assertThat(firstElement.getSellerId()).isEqualTo(sellerId);
        assertThat(firstElement.getBuyerId()).isNull();
        assertThat(firstElement.getPrice()).isEqualTo(price);
        assertThat(firstElement.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(firstElement.getCreatedAt()).isNotNull();
        assertThat(firstElement.getClosedAt()).isNull();
    }
}
