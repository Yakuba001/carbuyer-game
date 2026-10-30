package com.carbuyer.user.api;

import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;

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
    void addBots(List<String> names, List<String> emails, List<String> passwords);
    long getNumberOfAllInActiveBots();
    long getNumberOfActiveBots();
    void activateBots(long amount);
    void deactivateBots(long amount);
}
