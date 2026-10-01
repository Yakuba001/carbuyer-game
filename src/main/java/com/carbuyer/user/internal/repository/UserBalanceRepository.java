package com.carbuyer.user.internal.repository;

import com.carbuyer.user.internal.entity.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

}
