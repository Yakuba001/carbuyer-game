package com.carbuyer.bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bot_config")
@Getter
@NoArgsConstructor
public class BotConfig {

    @Id
    private Integer id = 1;

    @Column(name = "total_bots", nullable = false)
    @Setter
    private int totalBots = 0;
}
