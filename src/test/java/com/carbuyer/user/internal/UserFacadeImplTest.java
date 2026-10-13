package com.carbuyer.user.internal;

import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserFacadeImplTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserFacadeImpl userFacadeImpl;

    @Test
    void existByEmail_whenEmailExists_returnTrue() {
        String existingEmail = "existingEmail";
        when(userRepository.existsByEmail(existingEmail)).thenReturn(true);

        boolean result = userFacadeImpl.existByEmail(existingEmail);

        assertThat(result).isTrue();
        verify(userRepository).existsByEmail(existingEmail);
    }

    @Test
    void existByEmail_whenEmailNotExists_returnFalse() {
        String notExistingEmail = "notExistingEmail";
        when(userRepository.existsByEmail(notExistingEmail)).thenReturn(false);

        boolean result = userFacadeImpl.existByEmail(notExistingEmail);

        assertThat(result).isFalse();
        verify(userRepository).existsByEmail(notExistingEmail);
    }

    @Test
    void existByUsername_whenUsernameExists_returnTrue() {
        String existingUsername = "existingUsername";
        when(userRepository.existsByName(existingUsername)).thenReturn(true);

        boolean result = userFacadeImpl.existByUsername(existingUsername);

        assertThat(result).isTrue();
        verify(userRepository).existsByName(existingUsername);
    }

    @Test
    void existByUsername_whenUsernameNotExists_returnFalse() {
        String notExistingUsername = "notExistingUsername";
        when(userRepository.existsByName(notExistingUsername)).thenReturn(false);

        boolean result = userFacadeImpl.existByUsername(notExistingUsername);

        assertThat(result).isFalse();
        verify(userRepository).existsByName(notExistingUsername);
    }

    @Test
    void save_shouldReturnSavedUserWithCorrectData() {
        String name = "name";
        String email = "email";
        String password = "password";
        User savedUser = new User(name, email, password);
        when(userRepository.save(any())).thenReturn(savedUser);

        User result = userFacadeImpl.save(name, email, password);

        assertThat(result).isEqualTo(savedUser);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getPassword()).isEqualTo(password);
        verify(userRepository).save(any());
    }

    @Test
    void findByEmail_whenEmailExists_shouldReturnUser() {
        String existingEmail = "existingEmail";
        User savedUser = new User("name", existingEmail, "password");
        when(userRepository.findByEmail(existingEmail)).thenReturn(Optional.of(savedUser));

        User result = userFacadeImpl.findByEmail(existingEmail);

        assertThat(result).isEqualTo(savedUser);
        assertThat(result.getName()).isEqualTo("name");
        assertThat(result.getEmail()).isEqualTo(existingEmail);
        assertThat(result.getPassword()).isEqualTo("password");
        verify(userRepository).findByEmail(existingEmail);
    }

    @Test
    void findByEmail_whenEmailNotExists_shouldThrowException() {
        String notExistingEmail = "notExistingEmail";
        when(userRepository.findByEmail(notExistingEmail)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userFacadeImpl.findByEmail(notExistingEmail))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findIdByUsername_whenUsernameNotExists_shouldThrowIllegalArgumentException() {
        String notExisting = "notExistingUsername";

        assertThatThrownBy(() -> userFacadeImpl.findIdByUsername(notExisting))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
