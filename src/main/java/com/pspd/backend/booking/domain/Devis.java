package com.pspd.backend.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Devis émis par le prestataire pour une réservation {@code AVEC_DEVIS} (Epic C).
 * Le prestataire consulte la demande (et ses images), puis chiffre : {@code montant},
 * {@code dureeEstimeeH}, {@code conditions}. Le client accepte ou refuse.
 *
 * <p>Mappé sur la table {@code devis} (déjà présente dès {@code V1__init.sql}) ;
 * un seul devis par réservation ({@code UNIQUE reservation_id}).</p>
 */
@Entity
@Table(name = "devis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devis {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "reservation_id", length = 36, nullable = false)
    private String reservationId;

    @Column(precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "duree_estimee_h", precision = 5, scale = 2)
    private BigDecimal dureeEstimeeH;

    @Column(columnDefinition = "TEXT")
    private String conditions;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private StatutDevis statut = StatutDevis.ENVOYE;

    @Column(name = "emis_le", nullable = false, updatable = false)
    private LocalDateTime emisLe;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.statut == null) this.statut = StatutDevis.ENVOYE;
        if (this.emisLe == null) this.emisLe = LocalDateTime.now();
    }
}
