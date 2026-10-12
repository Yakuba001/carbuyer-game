package com.carbuyer.user.internal.service;

import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
import com.carbuyer.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@RecordApplicationEvents
@Transactional
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
    private UserBalanceRepository userBalanceRepository;
    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void createUser_withCorrectAndUniqueNameAndEmail_shouldReturnUserDto_andSendAnEventIntoGarageModule() {
        CreateUserCommand cmd = new CreateUserCommand("Name", "name@g.com", "123user");

        UserDto result = userService.createUser(cmd);

        User savedUser = userRepository.findByEmail(cmd.email()).orElseThrow(RuntimeException::new);
        UserBalance savedBalance = userBalanceRepository.findById(savedUser.getId()).orElseThrow(RuntimeException::new);
        List<UserCreatedEvent> eventList = applicationEvents
                .stream(UserCreatedEvent.class)
                .filter(event -> event.userId().equals(result.id()))
                .toList();

        assertThat(savedUser.getEmail()).isEqualTo(cmd.email());
        assertThat(savedUser.getName()).isEqualTo(cmd.name());
        assertThat(savedUser.getBalance().getBalance()).isEqualTo(savedBalance.getBalance());

        assertThat(eventList)
                .hasSize(1)
                .element(0)
                .satisfies(event -> {
                    assertThat(event.userId()).isEqualTo(result.id());
                    assertThat(event.maxSlots()).isEqualTo(result.maxSlots());
                });

        assertThat(result.id()).isEqualTo(savedUser.getId());
        assertThat(result.name()).isEqualTo(savedUser.getName());
        assertThat(result.email()).isEqualTo(savedUser.getEmail());
        assertThat(result.balance()).isEqualTo(savedUser.getBalance().getBalance());
        assertThat(result.maxSlots()).isEqualTo(savedUser.getMaxSlots());
        assertThat(result.createdAt()).isEqualTo(savedUser.getCreatedAt());
    }

    @Test
    void createUser_withNotUniqueName_shouldThrowDataIntegrityViolationException_andReturnIllegalArgumentException() {
        CreateUserCommand cmd1 = new CreateUserCommand("Name", "name@g.com", "123user");
        CreateUserCommand cmd2 = new CreateUserCommand("Name", "name1@g.com", "123user");

        userService.createUser(cmd1);
        assertThatThrownBy(() -> userService.createUser(cmd2)).isInstanceOf(IllegalArgumentException.class);

        List<UserCreatedEvent> eventList = applicationEvents.stream(UserCreatedEvent.class).toList();

        assertThat(eventList).hasSize(1);
    }

    @Test
    void createUser_withNotUniqueEmail_shouldThrowDataIntegrityViolationException_andReturnIllegalArgumentException() {
        CreateUserCommand cmd1 = new CreateUserCommand("Name", "name@g.com", "123user");
        CreateUserCommand cmd2 = new CreateUserCommand("Name1", "name@g.com", "123user");

        userService.createUser(cmd1);
        assertThatThrownBy(() -> userService.createUser(cmd2)).isInstanceOf(IllegalArgumentException.class);
        List<UserCreatedEvent> eventList = applicationEvents.stream(UserCreatedEvent.class).toList();

        assertThat(eventList).hasSize(1);
    }
}
