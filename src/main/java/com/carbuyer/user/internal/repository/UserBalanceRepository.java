package com.carbuyer.user.internal.repository;

import com.carbuyer.user.internal.entity.UserBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserBalance ub SET ub.balance = ub.balance - :amount " +
            "WHERE ub.userId = :buyerId AND ub.balance >= :amount")
    int minusBalanceIfSufficient(@Param("buyerId") Long buyerId, @Param("amount") BigDecimal amount);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserBalance ub SET ub.balance = ub.balance + :amount " +
            "WHERE ub.userId = :sellerId")
    int plusBalance(@Param("sellerId") Long sellerId, @Param("amount") BigDecimal amount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ub FROM UserBalance ub WHERE ub.userId = :userId")
    Optional<UserBalance> findBalanceByUserId(@Param("userId") Long userId);
}
