package com.carbuyer.user.internal;

import com.carbuyer.user.api.UserFacade;
import com.carbuyer.user.api.dto.CreateUserCommand;
import com.carbuyer.user.api.dto.UserDto;
import com.carbuyer.user.internal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final UserService service;

    @Override
    public boolean existsByEmail(String email) {
        return service.isUserExistsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return service.isUserExistsByName(username);
    }

    @Override
    public UserDetails save(String username, String email, String password) {
        UserDto user = service.createUser(new CreateUserCommand(username, email, password));
        return User.builder()
                .username(user.email())
                .password(password)
                .roles("USER")
                .build();
    }

    @Override
    public UserDetails findByEmail(String email) {
        return User.builder()
                .username(email)
                .password("")
                .roles("USER")
                .build();
    }

    @Override
    public Long findUserIdByEmail(String email) {
        return service.findIdByEmail(email);
    }

    @Override
    public boolean existsById(Long id) {
        return service.existsById(id);
    }

    @Override
    public BigDecimal getUserBalanceById(Long id) {
        return service.getUserBalanceById(id);
    }

    @Override
    public boolean carPurchase(Long userId, Long sellerId,
                               BigDecimal buyerBalance,
                               BigDecimal sellerBalance,
                               BigDecimal amount) {
        return service.carPurchase(userId, sellerId, buyerBalance, sellerBalance, amount);
    }
}
