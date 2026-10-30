package com.carbuyer.user.internal.service;

import com.carbuyer.exception.exceptions.*;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
import com.carbuyer.user.internal.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    @Value("${app.finance.initial-balance:0.00}")
    private BigDecimal initialBalance;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public UserDto createUser(CreateUserCommand command) {
        validateInputData(command);
        User newUser = new User(command.name(), command.email(), command.rawPassword());
        UserBalance newBalance = new UserBalance(initialBalance);
        newUser.setBalance(newBalance);
        try {
            User savedUser = userRepository.saveAndFlush(newUser);
            applicationEventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), savedUser.getMaxSlots()));
            return new UserDto(
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail(),
                    savedUser.getBalance().getBalance(),
                    savedUser.getMaxSlots(),
                    savedUser.getCreatedAt());
        } catch (DataIntegrityViolationException e) {
            dataBaseExceptionResolver(e);
            throw e;
        }
    }

    @Transactional
    public void createUsers(List<CreateUserCommand> commands) {
        List<User> users = commands.stream()
                .map(cmd -> {
                    User newUser = new User(cmd.name(), cmd.email(), cmd.rawPassword());
                    UserBalance newBalance = new UserBalance(initialBalance);
                    newUser.setBalance(newBalance);
                    newUser.setBot(true);
                    newUser.setBotActive(true);
                    return newUser;
                })
                .toList();
        try {
            List<User> savedUsers = userRepository.saveAllAndFlush(users);
            savedUsers.forEach(user ->
                    applicationEventPublisher.publishEvent(new UserCreatedEvent(user.getId(), user.getMaxSlots())));
        } catch (DataIntegrityViolationException e) {
            dataBaseExceptionResolver(e);
            throw e;
        }
    }

    public boolean isUserExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUserExistsByName(String name) {
        return userRepository.existsByName(name);
    }

    public Long findIdByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User with this email: " + email + " does not exists"));
        return user.getId();
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public BigDecimal getUserBalanceById(Long id) {
        UserBalance balance = userBalanceRepository.findByUserId(id)
                .orElseThrow(() -> new UserNotFoundException("User with this id: " + id + " does not exists"));
        return balance.getBalance();
    }

    @Transactional
    public boolean carPurchase(Long buyerId,
                               Long sellerId,
                               BigDecimal buyerBalance,
                               BigDecimal sellerBalance,
                               BigDecimal amount) {

        BigDecimal newBuyerBalance = buyerBalance.subtract(amount);
        BigDecimal newSellerBalance = sellerBalance.add(amount);
        int rowsDebited = userBalanceRepository.updateBalance(buyerId, newBuyerBalance);
        if (rowsDebited == 0) {
            return false;
        }
        int rowsCredited = userBalanceRepository.updateBalance(sellerId, newSellerBalance);
        if (rowsCredited == 0) {
            throw new UserNotFoundException("User with this id: " + sellerId + " does not exists");
        }
        return true;
    }

    public long getNumberOfActiveBots() {
        return userRepository.countByIsBotTrueAndIsBotActiveTrue();
    }

    public long getNumberOfInActiveBots() {
        return userRepository.countByIsBotTrueAndIsBotActiveFalse();
    }

    public void botActivator(long amount) {
        int result = userRepository.activateBots(amount);
        if (result == 0) throw new BotActivateException("Bot activation failed");
    }

    public void botDeactivator(long amount) {
        int result = userRepository.deactivateBots(amount);
        if (result == 0) throw new BotDeactivateException("Bot deactivation failed");
    }

    private void dataBaseExceptionResolver(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException err) {
            String constraintName = err.getConstraintName();
            if ("users_email_key".equals(constraintName)) {
                throw new EmailAlreadyExistsException("Email already exists");
            }
            if ("users_name_key".equals(constraintName)) {
                throw new NameAlreadyExistsException("Name already exists");
            }
        }
        throw new UserAlreadyExistsException("User data conflict");
    }

    private void validateInputData(CreateUserCommand command) {
        if (command.name() != null && command.name().startsWith("CarBuyerBot")) {
            throw new InvalidBotDataException("Username cannot start with system bot prefix");
        }
        if (command.email() != null && command.email().endsWith("@carbuyer.com")) {
            throw new InvalidBotDataException("Email domain is reserved for system bots");
        }
    }
}
