package com.carbuyer.user.internal.service;

import com.carbuyer.exception.exceptions.*;
import com.carbuyer.garage.api.GarageFacade;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.internal.entity.User;
import com.carbuyer.user.internal.entity.UserBalance;
import com.carbuyer.user.internal.repository.UserBalanceRepository;
import com.carbuyer.user.internal.repository.UserRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.exception.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    @Value("${app.finance.initial-balance:0.00}")
    private BigDecimal initialBalance;
    private final GarageFacade garageFacade;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String BOTS_DOMAIN = "@carbuyer.com";
    private static final String BOTS_PREFIX = "CarBuyerBot";

    @Transactional
    public UserDto createUser(CreateUserCommand command) {
        validateInputData(command);
        User newUser = new User(command.name(), command.email(), command.rawPassword());
        UserBalance newBalance = new UserBalance(initialBalance);
        newUser.setBalance(newBalance);
        int garageDefaultSlotNumber = 1;
        try {
            User savedUser = userRepository.saveAndFlush(newUser);
            garageFacade.addGarageSlot(savedUser.getId(), garageDefaultSlotNumber);
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
    public List<Long> createUsers(int amount) {
        String botPassWord = passwordGenerator();
        List<User> users = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            User newUser = new User(uniqueFieldGenerator("name"), uniqueFieldGenerator("email"), botPassWord);
            UserBalance newBalance = new UserBalance(initialBalance);
            newUser.setBalance(newBalance);
            newUser.setBot(true);
            users.add(newUser);
        }
        try {
            MapSqlParameterSource[] userParams = users.stream()
                    .map(user -> {
                        MapSqlParameterSource params = new MapSqlParameterSource();
                        params.addValue("name", user.getName());
                        params.addValue("email", user.getEmail());
                        params.addValue("password", user.getPassword());
                        params.addValue("isBot", user.isBot());
                        return params;
                    }).toArray(MapSqlParameterSource[]::new);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            String userSql =
                    "INSERT INTO users (name, email, password_hash, is_bot) VALUES (:name, :email, :password, :isBot)";
            String balanceSql =
                    "INSERT INTO user_balances (user_id, balance) VALUES (:userId, :balance)";
            jdbcTemplate.batchUpdate(userSql, userParams, keyHolder, new String[]{"id"});
            List<Long> savedUserIds = keyHolder.getKeyList().stream()
                    .map(map -> ((Number) map.get("id")).longValue()).toList();
            MapSqlParameterSource[] userBalanceParams = savedUserIds.stream()
                    .map(userId -> {
                        MapSqlParameterSource param = new MapSqlParameterSource();
                        param.addValue("userId", userId);
                        param.addValue("balance", initialBalance);
                        return param;
                    }).toArray(MapSqlParameterSource[]::new);
            jdbcTemplate.batchUpdate(balanceSql, userBalanceParams);
            garageFacade.addGarageSlots(savedUserIds);
            return savedUserIds;
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

    public List<Long> getAllBuyerBotIds() {
        return userRepository.findBuyerBotIds();
    }

    public List<Long> getAllSellerBotIds() {
        return userRepository.findActiveSellerBotIds();
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
        if (command.name() != null && command.name().startsWith(BOTS_PREFIX)) {
            throw new InvalidBotDataException("Username cannot start with system bot prefix");
        }
        if (command.email() != null && command.email().endsWith(BOTS_DOMAIN)) {
            throw new InvalidBotDataException("Email domain is reserved for system bots");
        }
    }

    private String uniqueFieldGenerator(String field) {
        switch (field) {
            case "name" -> {
                return BOTS_PREFIX + saltGenerator();
            }
            case "email" -> {
                return "email" + saltGenerator() + BOTS_DOMAIN;
            }
            default -> throw new InvalidBotDataException("Invalid field provided");
        }
    }

    private String passwordGenerator() {
        return passwordEncoder.encode("botPassHardEnough" + saltGenerator());
    }

    private String saltGenerator() {
        return RandomStringUtils.randomAlphanumeric(8);
    }
}
