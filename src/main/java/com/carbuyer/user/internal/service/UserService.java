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
import org.springframework.dao.DataIntegrityViolationException;
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
        User newUser = new User(command.name(), command.email(), command.rawPassword());
        UserBalance newBalance = new UserBalance(initialBalance);
        newUser.setBalance(newBalance);
        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("User with this email or name already exists");
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
}
