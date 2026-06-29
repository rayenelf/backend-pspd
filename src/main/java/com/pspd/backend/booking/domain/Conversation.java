package com.pspd.backend.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reservation_id", length = 36)
    private String reservationId;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.creeLe == null) this.creeLe = LocalDateTime.now();
    }
}
