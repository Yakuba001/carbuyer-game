package com.carbuyer.user.internal.service;

import com.carbuyer.exception.exceptions.*;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
import com.carbuyer.user.internal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private UserBalanceRepository userBalanceRepository;
    @Mock
    private GarageFacade garageFacade;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldThrowException_whenEmailOrNameIsTaken() {
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() ->
                userService.createUser(new CreateUserCommand("name", "email", "password")))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository).saveAndFlush(any(User.class));
        verifyNoInteractions(garageFacade);
    }

    @Test
    void createUser_shouldSuccess_whenDataIsValid() {
        User stubUser = new User("name", "email", "password");
        UserBalance stubBalance = new UserBalance(new BigDecimal("11.11"));
        stubUser.setBalance(stubBalance);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(stubUser);
        doNothing().when(garageFacade).addGarageSlot(any(), anyInt());

        userService.createUser(new CreateUserCommand("name", "email", "password"));

        verify(userRepository).saveAndFlush(any(User.class));
        verify(garageFacade).addGarageSlot(any(), anyInt());
    }

    @Test
    void createUser_shouldThrowInvalidBotDataException_whenUserTryAddedReservedWordsInHisName() {
        CreateUserCommand cmd = new CreateUserCommand("CarBuyerBot", "email", "password");

        assertThatThrownBy(() -> userService.createUser(cmd))
                .isInstanceOf(InvalidBotDataException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void createUser_shouldThrowInvalidBotDataException_whenUserTryAddedReservedDomainInHisEmail() {
        CreateUserCommand cmd = new CreateUserCommand("name", "@carbuyer.com", "password");

        assertThatThrownBy(() -> userService.createUser(cmd))
                .isInstanceOf(InvalidBotDataException.class);

        verifyNoInteractions(userRepository);
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
    void findIdByUsername_whenEmailNotExists_shouldThrowIllegalArgumentException() {
        String notExisting = "notExistingUsername";

        assertThatThrownBy(() -> userService.findIdByEmail(notExisting))
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
        UserBalance testUserBalance = new UserBalance(balance);
        when(userBalanceRepository.findByUserId(id)).thenReturn(Optional.of(testUserBalance));

        BigDecimal result = userService.getUserBalanceById(id);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(balance);
    }

    @Test
    void carPurchase_shouldThrownException_whenSellerDoesNotExists() {
        BigDecimal testAmount = new BigDecimal("228.21");
        BigDecimal buyerBalance = new BigDecimal("228.21");
        BigDecimal sellerBalance = new BigDecimal("228.21");
        long buyerId = 1L;
        long sellerId = 2L;
        when(userBalanceRepository.updateBalance(eq(buyerId), any(BigDecimal.class))).thenReturn(1);
        when(userBalanceRepository.updateBalance(eq(sellerId), any(BigDecimal.class))).thenReturn(0);

        assertThatThrownBy(() -> userService.carPurchase(buyerId, sellerId, buyerBalance, sellerBalance, testAmount))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void carPurchase_shouldReturnTrue_whenBuyerHasEnoughAmountInBalanceAndSellerExists() {
        BigDecimal testAmount = new BigDecimal("228.21");
        BigDecimal buyerBalance = new BigDecimal("228.21");
        BigDecimal sellerBalance = new BigDecimal("228.21");
        long buyerId = 1L;
        long sellerId = 2L;
        when(userBalanceRepository.updateBalance(eq(buyerId), any(BigDecimal.class))).thenReturn(1);
        when(userBalanceRepository.updateBalance(eq(sellerId), any(BigDecimal.class))).thenReturn(1);

        boolean result = userService.carPurchase(buyerId, sellerId, buyerBalance, sellerBalance, testAmount);

        assertThat(result).isTrue();
    }

    @Test
    void createUsers_shouldReturnCorrectIdsList_whenUsersAreCreated() {
        int amount = 2;
        long id1 = 10L;
        long id2 = 20L;
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(2);
            keyHolder.getKeyList().add(Map.of("id", id1));
            keyHolder.getKeyList().add(Map.of("id", id2));
            return new int[]{1, 1};
        }).when(jdbcTemplate).batchUpdate(
                any(String.class),
                any(MapSqlParameterSource[].class),
                any(KeyHolder.class),
                eq(new String[]{"id"})
        );
        doAnswer(invocation -> new int[]{1, 1})
                .when(jdbcTemplate).batchUpdate(
                        any(String.class),
                        any(MapSqlParameterSource[].class));
        doNothing().when(garageFacade).addGarageSlots(anyList());

        List<Long> resultList = userService.createUsers(amount);

        assertThat(resultList).hasSize(amount);
        assertThat(resultList.get(0)).isEqualTo(id1);
        assertThat(resultList.get(1)).isEqualTo(id2);
    }

    @Test
    void createUsers_shouldThrowException_whenEmailOrNameIsTaken() {
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(jdbcTemplate).batchUpdate(
                        any(String.class),
                        any(MapSqlParameterSource[].class),
                        any(KeyHolder.class),
                        any(String[].class));

        assertThatThrownBy(() -> userService.createUsers(1))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void getAllBuyerBotIds_shouldReturnCorrectBuyerIdsList() {
        List<Long> expectedList = List.of(1L, 2L, 3L);
        when(userRepository.findBuyerBotIds()).thenReturn(expectedList);

        List<Long> resultList = userService.getAllBuyerBotIds();

        assertThat(resultList.size()).isEqualTo(expectedList.size());
    }

    @Test
    void getAllSellerBotIds_shouldReturnCorrectSellerIdsList() {
        List<Long> expectedList = List.of(1L, 2L, 3L);
        when(userRepository.findActiveSellerBotIds()).thenReturn(expectedList);

        List<Long> resultList = userService.getAllSellerBotIds();

        assertThat(resultList.size()).isEqualTo(expectedList.size());
    }
}
