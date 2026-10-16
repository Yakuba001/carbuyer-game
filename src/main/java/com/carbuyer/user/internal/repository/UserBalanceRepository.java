package com.carbuyer.user.internal.repository;

import com.carbuyer.user.internal.entity.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE UserBalance ub SET ub.balance = ub.balance - :amount " +
            "WHERE ub.userId = :buyerId AND ub.balance >= :amount")
    int minusBalanceIfSufficient(@Param("buyerId") Long buyerId, @Param("amount") BigDecimal amount);
    @Modifying
    @Transactional
    @Query("UPDATE UserBalance ub SET ub.balance = ub.balance + :amount " +
            "WHERE ub.userId = :sellerId")
    int plusBalance(@Param("sellerId") Long sellerId, @Param("amount") BigDecimal amount);
}
