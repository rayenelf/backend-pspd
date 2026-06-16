package com.pspd.backend.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Réservation d'un service par un client auprès d'un prestataire (Epic C).
 * Le cycle de vie suit {@link StatutReservation} / {@code 12-state-reservation.png}.
 * {@code prixConvenu} est renseigné par le prestataire au moment de l'acceptation.
 *
 * <p>Mappée sur la table {@code reservations} créée dès {@code V1__init.sql}
 * (le schéma complet existe déjà ; pas de nouvelle migration en Sprint 3).</p>
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "client_id", length = 36, nullable = false)
    private String clientId;

    @Column(name = "prestataire_id", length = 36, nullable = false)
    private String prestataireId;

    @Column(name = "service_id", length = 36, nullable = false)
    private String serviceId;

    @Column(name = "adresse_id", length = 36)
    private String adresseId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private TypeReservation type = TypeReservation.IMMEDIATE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    @Column(name = "date_service")
    private LocalDate dateService;

    @Column(name = "heure_service")
    private LocalTime heureService;

    /** Précisions libres du client sur le besoin (optionnel). */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Prix final convenu, fixé à l'acceptation par le prestataire (null tant que EN_ATTENTE). */
    @Column(name = "prix_convenu", precision = 12, scale = 2)
    private BigDecimal prixConvenu;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID().toString();
        if (this.statut == null) this.statut = StatutReservation.EN_ATTENTE;
        if (this.type == null) this.type = TypeReservation.IMMEDIATE;
        if (this.creeLe == null) this.creeLe = LocalDateTime.now();
    }
}
