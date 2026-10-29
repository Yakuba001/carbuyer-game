package com.carbuyer.user.internal.repository;

import com.carbuyer.containers.DataJpaIntegrationTest;
import com.carbuyer.user.internal.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class UserRepositoryIT extends DataJpaIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest
    @MethodSource(value = "duplicateCheckerUsers")
    void checkUniqueNameEmailFields_shouldThrowDataViolationException_whenFieldsUnUnique(User user) {
        addUserToDb("test", "test", "test");

        assertThatThrownBy(() -> userRepository.save(user)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @MethodSource(value = "nullCheckerUsers")
    void checkNotNullConstraints_shouldThrowDataViolationException_whenFieldsAreNull(User user) {
        assertThatThrownBy(() -> userRepository.save(user)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkDefaultsAmounts_shouldSetIdMaxSlotsVersionCreatedAt_whenUserAdded() {
        User user = new User("test", "test", "test");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getMaxSlots()).isEqualTo(1);
        assertThat(savedUser.getVersion()).isEqualTo(0);
        assertThat(savedUser.getCreatedAt()).isNotNull();
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

    @Test
    void countByEmailEndingWith_returnsCorrectValue_whenUsersWithThisDomainExists() {
        String testDomain = "@carbuyer.com";
        addUserToDb("Joe", "a@gmail.com", "test");
        addUserToDb("Ben", "ben@carbuyer.com", "test");
        addUserToDb("Bob", "bob@carbuyer.com", "test");

        long result = userRepository.countByEmailEndingWith(testDomain);

        assertThat(result).isEqualTo(2);
    }

    private void addUserToDb(String name, String email, String password) {
        User savedUser = new User(name, email, password);
        userRepository.save(savedUser);
    }

    private static Stream<User> nullCheckerUsers() {
        return Stream.of(
                new User(null, "test1", "test2"),
                new User("test", null, "test2"),
                new User("test", "test1", null)
        );
    }

    private static Stream<User> duplicateCheckerUsers() {
        return Stream.of(
                new User("test", "test1", "test1"),
                new User("test1", "test", "test1")
        );
    }
}
