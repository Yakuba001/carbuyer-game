package com.carbuyer.user.internal.service;

import com.carbuyer.exception.exceptions.UserAlreadyExistsException;
import com.carbuyer.exception.exceptions.UserNotFoundException;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks
    private UserService userService;

    @Test
    void createUser_ShouldThrowException_WhenEmailOrNameIsTaken() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() ->
                userService.createUser(new CreateUserCommand("name", "email", "password")))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository).saveAndFlush(any(User.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createUser_ShouldSuccess_WhenDataIsValid() {
        User stubUser = new User("name", "email", "password");
        UserBalance stubBalance = new UserBalance(new BigDecimal("11.11"));
        stubUser.setBalance(stubBalance);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(stubUser);

        userService.createUser(new CreateUserCommand("name", "email", "password"));

        verify(userRepository).saveAndFlush(any(User.class));
        verify(applicationEventPublisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void existByEmail_whenEmailExists_returnTrue() {
        String existingEmail = "existingEmail";
        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

        boolean result = userService.isUserExistsByEmail(existingEmail);

        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(existingEmail);
    }

    @Test
    void existByEmail_whenEmailNotExists_returnFalse() {
        String notExistingEmail = "notExistingEmail";
        when(userRepository.existsByEmail(notExistingEmail)).thenReturn(false);

        boolean result = userService.isUserExistsByEmail(notExistingEmail);

        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(notExistingEmail);
    }

    @Test
    void existByUsername_whenUsernameExists_returnTrue() {
        String existingUsername = "existingUsername";
        when(userRepository.existsByName(existingUsername)).thenReturn(true);

        boolean result = userService.isUserExistsByName(existingUsername);

        assertThat(result).isTrue();
        verify(userRepository).existsByName(existingUsername);
    }

    @Test
    void existByUsername_whenUsernameNotExists_returnFalse() {
        String notExistingUsername = "notExistingUsername";
        when(userRepository.existsByName(notExistingUsername)).thenReturn(false);

        boolean result = userService.isUserExistsByName(notExistingUsername);

        assertThat(result).isFalse();
        verify(userRepository).existsByName(notExistingUsername);
    }

    @Test
    void save_shouldReturnSavedUserWithCorrectData() {
        String name = "name";
        String email = "email";
        String password = "password";
        long fakeId = 1L;
        CreateUserCommand savedUserCommand = new CreateUserCommand(name, email, password);
        User savedUser = new User(savedUserCommand.name(), savedUserCommand.email(), savedUserCommand.rawPassword());
        ReflectionTestUtils.setField(savedUser, "id", fakeId);
        UserBalance fakeBalance = new UserBalance(new BigDecimal("0.00"));
        savedUser.setBalance(fakeBalance);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.createUser(savedUserCommand);

        assertThat(result.id()).isEqualTo(fakeId);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.email()).isEqualTo(email);
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void findByEmail_whenEmailExists_shouldReturnUser() {
        String existingEmail = "existingEmail";
        User savedUser = new User("name", existingEmail, "password");
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(savedUser));

        String result = userService.getUsernameByEmail(existingEmail);

        assertThat(result).isEqualTo("name");
        verify(userRepository).findByEmail(existingEmail);
    }

    @Test
    void findByEmail_whenEmailNotExists_shouldThrowException() {
        String notExistingEmail = "notExistingEmail";
        when(userRepository.findByEmail(notExistingEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUsernameByEmail(notExistingEmail))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findIdByUsername_whenUsernameNotExists_shouldThrowIllegalArgumentException() {
        String notExisting = "notExistingUsername";

        assertThatThrownBy(() -> userService.findIdByUsername(notExisting))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void existsById_whenUserWithInputIdExists_returnTrue() {
        long id = 1L;
        when(userRepository.existsById(id)).thenReturn(true);

        boolean result = userService.existsById(id);

        assertThat(result).isTrue();
    }

    @Test
    void getUserBalanceById_whenUserWithInputIdExists_returnUserBalance() {
        long id = 1L;
        BigDecimal balance = new BigDecimal("228.21");
        User testUser = new User("Boss", "boss@g.c", "pass123");
        UserBalance testUserBalance = new UserBalance(balance);
        testUser.setBalance(testUserBalance);
        when(userRepository.findById(id)).thenReturn(Optional.of(testUser));

        BigDecimal result = userService.getUserBalanceById(id);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(balance);
    }
}
