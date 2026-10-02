package com.carbuyer.user.internal.service;

import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
public class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GarageSlotRepository garageSlotRepository;

    @AfterEach
    void tearDown() {
        garageSlotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldSaveUserAndCreateGarageSlots_InRealDatabase() {
        CreateUserCommand command = new CreateUserCommand("name", "email", "password");

        UserDto resultDto = userService.createUser(command);
        User user = userRepository.findById(resultDto.id()).orElseThrow(() -> new RuntimeException("User not found"));

        assertThat(userRepository.existsByName(command.name())).isTrue();
        assertThat(userRepository.existsByEmail(command.email())).isTrue();
        assertThat(garageSlotRepository.count()).isEqualTo(1);

        assertThat(resultDto).isNotNull();
        assertThat(resultDto.balance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(resultDto.maxSlots()).isEqualTo(1);
        assertThat(resultDto.name()).isEqualTo(command.name());
        assertThat(resultDto.email()).isEqualTo(command.email());
        assertThat(resultDto.createdAt()).isNotNull();
        assertThat(resultDto.id()).isNotNull();

        assertThat(user).isNotNull();
        assertThat(user.getMaxSlots()).isEqualTo(resultDto.maxSlots());
        assertThat(user.getName()).isEqualTo(resultDto.name());
        assertThat(user.getEmail()).isEqualTo(resultDto.email());
        assertThat(user.getCreatedAt()).isNotNull();
    }
}
