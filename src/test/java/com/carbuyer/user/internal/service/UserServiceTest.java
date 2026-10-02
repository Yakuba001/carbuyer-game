package com.carbuyer.user.internal.service;

import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    void createUser_ShouldThrowException_WhenEmailIsTaken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        when(userRepository.existsByName(anyString())).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(new CreateUserCommand("name", "email", "password")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository).existsByEmail(anyString());
        verify(userRepository).existsByName(anyString());
        verify(userRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createUser_ShouldThrowException_WhenNameIsTaken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByName(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(new CreateUserCommand("name", "email", "password")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository).existsByEmail(anyString());
        verify(userRepository).existsByName(anyString());
        verify(userRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void createUser_ShouldSuccess_WhenDataIsValid() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByName(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.createUser(new CreateUserCommand("name", "email", "password"));

        verify(userRepository).existsByEmail(anyString());
        verify(userRepository).existsByName(anyString());
        verify(userRepository).save(any(User.class));
        verify(applicationEventPublisher).publishEvent(any(UserCreatedEvent.class));
    }
}
