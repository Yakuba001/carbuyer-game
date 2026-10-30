package com.carbuyer.bot.repository;

import com.carbuyer.bot.entity.BotConfig;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BotConfigRepository extends JpaRepository<BotConfig, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BotConfig c WHERE c.id = 1")
    Optional<BotConfig> findAndLock();
}
