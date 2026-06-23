package com.pspd.backend.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Image de travail jointe par le client à une réservation {@code AVEC_DEVIS}
 * (photos du besoin à chiffrer). Consultée par le prestataire avant d'émettre
 * son devis.
 *
 * <p>Le fichier est stocké sur disque via {@code FileStorageService} ; seule
 * l'URL relative ({@code /uploads/...}) est conservée ici. Table créée en
 * {@code V13__reservation_images.sql}.</p>
 */
@Entity
@Table(name = "reservation_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationImage {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reservation_id", length = 36, nullable = false)
    private String reservationId;

    @Column(length = 400, nullable = false)
    private String url;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.ordre == null) this.ordre = 0;
        if (this.creeLe == null) this.creeLe = LocalDateTime.now();
    }
}
