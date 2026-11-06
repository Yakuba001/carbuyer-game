package com.carbuyer.bot.entity;

import jakarta.persistence.*;
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

    @Column(name = "active_bots", nullable = false)
    @Setter
    private int activeBots = 0;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter
    private ConfigStatus status = ConfigStatus.NOT_ADDED;
}
