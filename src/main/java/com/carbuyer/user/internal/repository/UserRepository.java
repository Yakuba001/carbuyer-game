package com.carbuyer.user.internal.repository;

import com.carbuyer.user.internal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    int countByIsBotTrueAndIsBotActiveTrue();
    int countByIsBotTrueAndIsBotActiveFalse();
    @Modifying
    @Query(value = """
    UPDATE users SET is_bot_active = true WHERE id IN (
        SELECT id FROM users WHERE is_bot = true AND is_bot_active = false LIMIT :amount)
    """, nativeQuery = true)
    int activateBots(@Param("amount") int amount);
    @Modifying
    @Query(value = """
    UPDATE users SET is_bot_active = false WHERE id IN (
            SELECT id FROM users WHERE is_bot = true AND is_bot_active = true LIMIT :amount)
    """, nativeQuery = true)
    int deactivateBots(@Param("amount") int amount);
    @Query(value = "SELECT DISTINCT u.id " +
            "FROM users u " +
            "JOIN garage_slots gs ON u.id = gs.user_id " +
            "WHERE u.is_bot = true " +
            "AND u.is_bot_active = true " +
            "AND gs.status = 'EMPTY' " +
            "AND gs.car_id IS NULL " +
            "LIMIT :limit", nativeQuery = true)
    List<Long> findActiveBotIdsWithLimit(@Param("limit") int limit);
}
