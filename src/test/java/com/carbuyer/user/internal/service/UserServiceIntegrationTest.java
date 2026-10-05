package com.carbuyer.user.internal.service;

import com.carbuyer.garage.internal.entity.GarageSlot;
import com.carbuyer.garage.internal.repository.GarageSlotRepository;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @AfterEach
    void tearDown() {
        garageSlotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldSaveUserAndCreateGarageSlots_InRealDatabase() {
        CreateUserCommand command = new CreateUserCommand("name", "email", "password");

        UserDto resultDto = userService.createUser(command);
        User user = userRepository.findWithBalanceById(resultDto.id())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserBalance balance = userBalanceRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Balance not found"));
        List<GarageSlot> garageSlots = garageSlotRepository.findByUserId(user.getId());

        assertThat(userRepository.existsByName(command.name())).isTrue();
        assertThat(userRepository.existsByEmail(command.email())).isTrue();
        assertThat(garageSlots.size()).isEqualTo(1);

        assertThat(resultDto).isNotNull();
        assertThat(resultDto.balance()).isEqualTo(user.getBalance().getBalance());
        assertThat(resultDto.maxSlots()).isEqualTo(user.getMaxSlots());
        assertThat(resultDto.name()).isEqualTo(user.getName());
        assertThat(resultDto.email()).isEqualTo(user.getEmail());
        assertThat(resultDto.createdAt()).isNotNull();
        assertThat(resultDto.id()).isEqualTo(user.getId());

        assertThat(balance.getBalance()).isEqualTo(user.getBalance().getBalance());
        assertThat(balance.getUserId()).isEqualTo(user.getId());
        assertThat(garageSlots.getFirst().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void createUser_ShouldThrowIllegalArgumentException_whenEmailOrNameAlreadyExists() {
        CreateUserCommand c1 = new CreateUserCommand("name1", "email1", "password");
        CreateUserCommand cEmail = new CreateUserCommand("name2", "email1", "password");
        CreateUserCommand cName = new CreateUserCommand("name1", "email2", "password");
        CreateUserCommand cBoth = new CreateUserCommand("name1", "email1", "password");

        userService.createUser(c1);

        assertThatThrownBy(() ->  userService.createUser(cName)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  userService.createUser(cEmail)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->  userService.createUser(cBoth)).isInstanceOf(IllegalArgumentException.class);
    }
}
