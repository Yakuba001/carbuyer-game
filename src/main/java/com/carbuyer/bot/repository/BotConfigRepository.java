package com.carbuyer.bot.repository;

import com.carbuyer.bot.entity.BotConfig;
import com.carbuyer.bot.entity.ConfigStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BotConfigRepository extends JpaRepository<BotConfig, Integer> {

    @Modifying
    @Query("UPDATE BotConfig c SET c.status = :newStatus WHERE c.id = 1 AND c.status = :expectedStatus")
    int updateStatusIfExpected(@Param("newStatus") ConfigStatus newStatus,
                               @Param("expectedStatus") ConfigStatus expectedStatus);
    @Query("SELECT c.status FROM BotConfig c WHERE c.id = 1")
    String getConfigStatus();
}
