package com.carbuyer.user.internal.service;

import com.carbuyer.user.api.dto.CreateUserCommand;
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

import java.math.BigDecimal;

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
                .isInstanceOf(IllegalArgumentException.class);

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
}
