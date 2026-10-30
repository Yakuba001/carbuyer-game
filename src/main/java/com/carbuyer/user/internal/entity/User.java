package com.carbuyer.user.internal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User implements UserDetails {

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
    @Setter
    @Column(name = "is_bot")
    private boolean isBot = false;
    @Setter
    @Column(name = "is_bot_active")
    private boolean isBotActive = false;
    @Setter
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
