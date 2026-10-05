package com.carbuyer.garage.internal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "garage_slots")
@NoArgsConstructor
@Getter
public class GarageSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "garage_slots_seq_gen")
    @SequenceGenerator(name = "garage_slots_seq_gen", sequenceName = "garage_slots_id_seq", allocationSize = 50)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Setter
    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;
    @Setter
    @Column(name = "car_id")
    private Long carId = null;
    @Setter
    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private GarageSlotStatus status = GarageSlotStatus.EMPTY;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public GarageSlot(Long userId, Integer slotNumber, Long carId, GarageSlotStatus status) {
        this.userId = userId;
        this.slotNumber = slotNumber;
        this.carId = carId;
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GarageSlot that)) return false;
        if (this.getId() == null || that.getId() == null) return false;
        return this.getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
