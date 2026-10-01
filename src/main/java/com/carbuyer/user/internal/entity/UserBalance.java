package com.carbuyer.user.internal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "user_balances")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserBalance {

    @Id
    @Column(name = "user_id")
    private Long userId;
    @Setter
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = new BigDecimal("0.00");
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public UserBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setUser(User user) {
        if (user != null) {
            this.user = user;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserBalance that)) return false;
        if (this.getUserId() == null || that.getUserId() == null) return false;
        return this.getUserId().equals(that.getUserId());
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
