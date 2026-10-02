package com.carbuyer.user.internal.service;

import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.api.event.UserCreatedEvent;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
import com.carbuyer.user.internal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
        boolean isEmailTaken = userRepository.existsByEmail(command.email());
        boolean isNameTaken = userRepository.existsByName(command.name());
        if (isEmailTaken) {
            throw new IllegalArgumentException("Email is already taken");
        }
        if (isNameTaken) {
            throw new IllegalArgumentException("Name is already taken");
        }
        User newUser = new User(command.name(), command.email(), command.rawPassword());
        UserBalance newBalance = new UserBalance(initialBalance);
        newUser.setBalance(newBalance);
        User savedUser = userRepository.save(newUser);
        applicationEventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId(), savedUser.getMaxSlots()));
        return new UserDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getBalance().getBalance(),
                savedUser.getMaxSlots(),
                savedUser.getCreatedAt());
    }
}
