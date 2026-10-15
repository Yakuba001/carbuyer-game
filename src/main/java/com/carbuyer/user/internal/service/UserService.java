package com.carbuyer.user.internal.service;

import com.carbuyer.exception.exceptions.EmailAlreadyExistsException;
import com.carbuyer.exception.exceptions.NameAlreadyExistsException;
import com.carbuyer.exception.exceptions.UserAlreadyExistsException;
import com.carbuyer.exception.exceptions.UserNotFoundException;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    @Value("${app.finance.initial-balance:0.00}")
    private BigDecimal initialBalance;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public UserDto createUser(CreateUserCommand command) {
        User newUser = new User(command.name(), command.email(), command.rawPassword());
        UserBalance newBalance = new UserBalance(initialBalance);
        newUser.setBalance(newBalance);
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException err) {
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
        applicationEventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), savedUser.getMaxSlots()));
        return new UserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getBalance().getBalance(),
                savedUser.getMaxSlots(),
                savedUser.getCreatedAt());
    }

    public boolean isUserExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUserExistsByName(String name) {
        return userRepository.existsByName(name);
    }

    public String getUsernameByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UserNotFoundException("User with this email: " + email + " does not exists"));
        return user.getName();
    }

    public Long findIdByUsername(String username) {
        User user = userRepository.findByName(username).orElseThrow(
                () -> new UserNotFoundException("User with this name: " + username + " does not exists"));
        return user.getId();
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public BigDecimal getUserBalanceById(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new UserNotFoundException("User with this id: " + id + " does not exists"));
        return user.getBalance().getBalance();
    }
}
