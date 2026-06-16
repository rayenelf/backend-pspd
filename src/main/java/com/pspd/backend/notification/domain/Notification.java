package com.pspd.backend.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification in-app destinée à un utilisateur (table {@code notifications}, créée en V1).
 * Version minimale Sprint 3 (notification client/prestataire aux transitions de réservation) ;
 * le canal push/email et le centre de notifications complet relèvent de la Phase 2 (Epic E).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "destinataire_id", length = 36, nullable = false)
    private String destinataireId;

    @Column(length = 10, nullable = false)
    @Builder.Default
    private String canal = "PUSH";

    @Column(length = 160)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    @Builder.Default
    private boolean lu = false;

    @Column(name = "envoyee_le", nullable = false, updatable = false)
    private LocalDateTime envoyeeLe;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.canal == null) this.canal = "PUSH";
        if (this.envoyeeLe == null) this.envoyeeLe = LocalDateTime.now();
    }
}
