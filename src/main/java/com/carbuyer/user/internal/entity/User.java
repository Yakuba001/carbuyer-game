package com.carbuyer.user.internal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    @Column(nullable = false, unique = true, length = 64)
    private String name;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Setter
    @Column(nullable = false, unique = true)
    private String email;
    @Setter
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Setter
    @Column(name = "max_slots")
    private Integer maxSlots = 1;
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, optional = false, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserBalance balance;

    public User(String name, String email, String passwordHash) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void setBalance(UserBalance balance) {
        this.balance = balance;
        if (balance != null && balance.getUser() != this) {
            balance.setUser(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User that)) return false;
        if (this.getId() == null || that.getId() == null) return false;
        return this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
