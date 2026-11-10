package com.carbuyer.user.internal.repository;

import com.carbuyer.user.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    @Query(value = """
    SELECT u.id FROM users u WHERE u.is_bot = true AND
    EXISTS(SELECT l FROM market_listings l WHERE l.seller_id = u.id AND l.status = 'ACTIVE')
    """, nativeQuery = true)
    List<Long> findActiveSellerBotIds();
    @Query(value = """
    SELECT u.id FROM users u WHERE u.is_bot = true AND
    NOT EXISTS(SELECT l FROM market_listings l WHERE l.seller_id = u.id AND l.status = 'ACTIVE')
    """, nativeQuery = true)
    List<Long> findBuyerBotIds();
}
