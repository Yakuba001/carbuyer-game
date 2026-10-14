package com.carbuyer.user.internal.repository;

import com.carbuyer.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class UserRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void checkUniqueNameField_shouldThrownDataViolationException_whenThisNameIsAlreadyExists() {
        addUserToDb("test", "test", "test1");
        User ununiqueUser = new User("test", "test1", "test2");

        assertThatThrownBy(() -> userRepository.save(ununiqueUser)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkUniqueEmailField_shouldThrownDataViolationException_whenThisEmailIsAlreadyExists() {
        addUserToDb("test", "test", "test3");
        User ununiqueUser = new User("test1", "test", "test4");

        assertThatThrownBy(() -> userRepository.save(ununiqueUser)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenUserWithThisEmailExists() {
        String email = "test";
        addUserToDb("test", email, "test5");

        boolean result = userRepository.existsByEmail(email);

        assertThat(result).isTrue();
    }

    @Test
    void existsByName_shouldReturnTrue_whenUserWithThisNameExists() {
        String name = "test";
        addUserToDb(name, "test", "test6");

        boolean result = userRepository.existsByName(name);

        assertThat(result).isTrue();
    }

    @Test
    void findByEmail_shouldReturnExistingUser() {
        String email = "test";
        addUserToDb("test", email, "test");

        Optional<User> user = userRepository.findByEmail(email);

        assertThat(user.isPresent()).isTrue();
    }

    @Test
    void findByName_shouldReturnExistingUser() {
        String name = "test";
        addUserToDb(name, "test", "test");

        Optional<User> user = userRepository.findByName(name);

        assertThat(user.isPresent()).isTrue();
    }

    private void addUserToDb(String name, String email, String password) {
        User savedUser = new User(name, email, password);
        userRepository.save(savedUser);
    }
}
