package com.carbuyer.user.internal.repository;

import com.carbuyer.user.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    @Query(value = """
    SELECT u.id FROM users u WHERE u.is_bot = true AND
    EXISTS(SELECT 1 FROM market_listings l WHERE l.seller_id = u.id AND l.status = 'ACTIVE')
    """, nativeQuery = true)
    List<Long> findActiveSellerBotIds();
    @Query(value = """
    SELECT u.id FROM users u WHERE u.is_bot = true AND
    NOT EXISTS(SELECT 1 FROM market_listings l WHERE l.seller_id = u.id AND l.status = 'ACTIVE')
    """, nativeQuery = true)
    List<Long> findBuyerBotIds();
    @Query(value = """
    SELECT u.id FROM (SELECT id FROM users WHERE is_bot = true ORDER BY id ASC LIMIT :limit)
    u WHERE EXISTS(SELECT 1 FROM market_listings l WHERE l.seller_id = u.id AND l.status = 'ACTIVE')
    """, nativeQuery = true)
    List<Long> findLimitedSellerBotIds(@Param("limit") int limit);
    @Query(value = """
    SELECT u.id FROM (SELECT id FROM users WHERE is_bot = true ORDER BY id ASC LIMIT :limit)
    u WHERE NOT EXISTS (SELECT 1 FROM market_listings l WHERE l.seller_id = u.id AND l.status = 'ACTIVE')
    """, nativeQuery = true)
    List<Long> findLimitedBuyerBotIds(@Param("limit") int limit);
}
