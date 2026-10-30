package com.carbuyer.user.internal.service;

import com.carbuyer.exception.exceptions.*;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
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
import java.util.List;
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
    void createUser_shouldThrowInvalidBotDataException_whenUserTryAddedReservedWordsInHisName() {
        CreateUserCommand cmd = new CreateUserCommand("CarBuyerBot", "email", "password");

        assertThatThrownBy(() -> userService.createUser(cmd))
                .isInstanceOf(InvalidBotDataException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void createUser_shouldThrowInvalidBotDataException_whenUserTryAddedReservedDomainInHisEmail() {
        CreateUserCommand cmd = new CreateUserCommand("name", "@carbuyer.com", "password");

        assertThatThrownBy(() -> userService.createUser(cmd))
                .isInstanceOf(InvalidBotDataException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(applicationEventPublisher);
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
    void createUsers_shouldCreateUsersAndSetUserIsBotTrueAndIsBotActiveTrue_whenMethodGetCorrectData() {
        User stubUser1 = new User("name", "email", "password");
        stubUser1.setBot(true);
        stubUser1.setBotActive(true);
        User stubUser2 = new User("name1", "email1", "password");
        stubUser2.setBot(true);
        stubUser2.setBotActive(true);
        CreateUserCommand cmd1 =
                new CreateUserCommand(stubUser1.getName(), stubUser1.getEmail(), stubUser1.getPassword());
        CreateUserCommand cmd2 =
                new CreateUserCommand(stubUser2.getName(), stubUser2.getEmail(), stubUser2.getPassword());
        List<User> stubUsers = List.of(stubUser1, stubUser2);
        UserBalance stubBalance = new UserBalance(new BigDecimal("11.11"));
        stubUsers.getFirst().setBalance(stubBalance);
        stubUsers.getLast().setBalance(stubBalance);
        when(userRepository.saveAllAndFlush(anyCollection())).thenReturn(stubUsers);

        userService.createUsers(List.of(cmd1, cmd2));

        assertThat(stubUser1.isBot()).isTrue();
        assertThat(stubUser1.isBotActive()).isTrue();
        assertThat(stubUser2.isBot()).isTrue();
        assertThat(stubUser2.isBotActive()).isTrue();

        verify(userRepository).saveAllAndFlush(anyCollection());
        verify(applicationEventPublisher, times(stubUsers.size())).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void createUsers_shouldThrowEmailAlreadyExistsException_whenEmailAlreadyExistsInDb() {
        CreateUserCommand cmd = new CreateUserCommand("name", "@g.com", "password");
        List<CreateUserCommand> stubUsers = List.of(cmd);
        when(userRepository.saveAllAndFlush(anyCollection())).thenThrow(EmailAlreadyExistsException.class);

        assertThatThrownBy(() -> userService.createUsers(stubUsers))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void createUsers_shouldThrowNameAlreadyExistsException_whenNameAlreadyExistsInDb() {
        CreateUserCommand cmd = new CreateUserCommand("name", "@g.com", "password");
        List<CreateUserCommand> stubUsers = List.of(cmd);
        when(userRepository.saveAllAndFlush(anyCollection())).thenThrow(NameAlreadyExistsException.class);

        assertThatThrownBy(() -> userService.createUsers(stubUsers))
                .isInstanceOf(NameAlreadyExistsException.class);

        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void createUsers_shouldThrowUserAlreadyExistsException_whenSomethingWentWrong() {
        CreateUserCommand cmd = new CreateUserCommand("name", "@g.com", "password");
        List<CreateUserCommand> stubUsers = List.of(cmd);
        when(userRepository.saveAllAndFlush(anyCollection())).thenThrow(UserAlreadyExistsException.class);

        assertThatThrownBy(() -> userService.createUsers(stubUsers))
                .isInstanceOf(UserAlreadyExistsException.class);

        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void getNumberOfActiveBots_shouldReturnCorrectNumberOfActiveBots_whenActiveBotsExists() {
        long amount = 1L;
        when(userRepository.countByIsBotTrueAndIsBotActiveTrue()).thenReturn(amount);

        long result = userService.getNumberOfActiveBots();

        assertThat(result).isEqualTo(amount);
    }

    @Test
    void getNumberOfInActiveBots_shouldReturnCorrectNumberOfInActiveBots_whenInActiveBotsExists() {
        long amount = 1L;
        when(userRepository.countByIsBotTrueAndIsBotActiveFalse()).thenReturn(amount);

        long result = userService.getNumberOfInActiveBots();

        assertThat(result).isEqualTo(amount);
    }

    @Test
    void botActivator_shouldThrowBotActivateException_whenRepositoryReturnsZero() {
        when(userRepository.activateBots(anyLong())).thenThrow(BotActivateException.class);

        assertThatThrownBy(() -> userService.botActivator(anyLong()))
                .isInstanceOf(BotActivateException.class);
    }

    @Test
    void botDeactivator_shouldThrowBotDeactivateException_whenRepositoryReturnsZero() {
        when(userRepository.deactivateBots(anyLong())).thenThrow(BotDeactivateException.class);

        assertThatThrownBy(() -> userService.botDeactivator(anyLong()))
                .isInstanceOf(BotDeactivateException.class);
    }
}
