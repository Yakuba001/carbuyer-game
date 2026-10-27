package com.carbuyer.user.api;

import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;

public interface UserFacade {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    UserDetails save(String username, String email, String password);
    UserDetails findByEmail(String email);
    Long findUserIdByEmail(String email);
    boolean existsById(Long id);
    BigDecimal getUserBalanceById(Long id);
    boolean carPurchase(Long buyerId,
                        Long sellerId,
                        BigDecimal buyerBalance,
                        BigDecimal sellerBalance,
                        BigDecimal amount);
}
